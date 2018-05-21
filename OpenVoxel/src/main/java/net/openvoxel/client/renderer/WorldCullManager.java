package net.openvoxel.client.renderer;

import net.openvoxel.OpenVoxel;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.utility.collection.CullingHashSet;
import net.openvoxel.utility.collection.IntDequeue;
import net.openvoxel.utility.debug.UsageAnalyses;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;

import java.util.function.Consumer;

class WorldCullManager {

	private final WorldDrawTask drawTask;

	WorldCullManager(WorldDrawTask drawTask) {
		this.drawTask = drawTask;
	}


	private static IntDequeue voxel_dequeue = new IntDequeue();
	private static CullingHashSet voxel_hash = new CullingHashSet();
	void runVoxelCull(int sizeLimit,Consumer<ClientChunkSection> consumer) {

		//Find Starting Chunk offset Position
		int startOffsetX = (int)Math.floor(drawTask.playerX / 16.0);
		int startOffsetY = (int)Math.floor(drawTask.playerY / 16.0);//TODO: ADD CAMERA OFFSET!!!!!!!!
		int startOffsetZ = (int)Math.floor(drawTask.playerZ / 16.0);

		internal_runFrustumCull(
				voxel_dequeue,
				voxel_hash,
				null,
				sizeLimit,
				startOffsetX,
				startOffsetY,
				startOffsetZ,
				consumer
				);
	}


	private static IntDequeue frustum_dequeue = new IntDequeue();
	private static CullingHashSet frustum_hash = new CullingHashSet();
	void runFrustumCull(Consumer<ClientChunkSection> consumer) {

		UsageAnalyses.StartCPUSample("View Frustum Cull",0);

		//Find Starting Chunk offset Position
		int startOffsetX = (int)Math.floor(drawTask.playerX / 16.0);
		int startOffsetY = (int)Math.floor(drawTask.playerY / 16.0);//TODO: ADD CAMERA OFFSET!!!!!!!!
		int startOffsetZ = (int)Math.floor(drawTask.playerZ / 16.0);

		//Call Culling Code
		internal_runFrustumCull(
				frustum_dequeue,
				frustum_hash,
				drawTask.frustumIntersect,
				drawTask.viewDistance,
				startOffsetX,
				startOffsetY,
				startOffsetZ,
				consumer
		);

		UsageAnalyses.StopCPUSample();
	}

	//TODO: STORE CONSTANT ARRAY SOMEWHERE!!
	void runShadowCull(Consumer<ClientChunkSection> consumer) {

		//Find Starting Chunk Offset Position
		int startOffsetX = 0;
		int startOffsetY = 0;
		int startOffsetZ = 0;

		internal_runFrustumCull(
				new IntDequeue(),
				new CullingHashSet(),
				drawTask.totalShadowIntersect,
				drawTask.viewDistance,
				startOffsetX,
				startOffsetY,
				startOffsetZ,
				consumer
		);
	}

	/**
	 * Perform Culling for a Frustum
	 *
	 * Based on: https://tomcc.github.io/2014/08/31/visibility-2.html
	 *
	 * @param sectionQueue must be empty() {will be returned empty}
	 * @param visitedOffsets must be empty() {will be returned empty}
	 * @param startOffsetX the starting chunk X in offset coordinates
	 * @param startOffsetY the starting chunk Y in offset coordinates
	 * @param startOffsetZ the starting chunk Z in offset coordinates
	 * @param consumer the function to be called when a valid chunk is found
	 */
	private void internal_runFrustumCull(
			@NotNull IntDequeue sectionQueue,
			@NotNull CullingHashSet visitedOffsets,
			@Nullable FrustumIntersection frustum,
			int viewDistance,
			int startOffsetX,
			int startOffsetY,
			int startOffsetZ,
			@NotNull Consumer<ClientChunkSection> consumer) {

		//Add Starting Chunk
		sectionQueue.add(startOffsetX);
		sectionQueue.add(startOffsetY);
		sectionQueue.add(startOffsetZ);
		sectionQueue.add(-1);
		sectionQueue.add(0);
		//sectionQueue.addLast(new CullSection(startOffsetX,startOffsetY,startOffsetZ,-1));
		visitedOffsets.add(startOffsetX,startOffsetY,startOffsetZ);

		//Constants
		final int[] xOffsets = BlockFace.array_xOffsets;
		final int[] yOffsets = BlockFace.array_yOffsets;
		final int[] zOffsets = BlockFace.array_zOffsets;
		final int[] opposite = BlockFace.array_opposite;
		CullSection section = new CullSection(0,0,0,0);

		//Breath First Search
		while(!sectionQueue.isEmpty()) {
			//CullSection section = sectionQueue.removeFirst();
			section.offsetPosX = sectionQueue.remove();
			section.offsetPosY = sectionQueue.remove();
			section.offsetPosZ = sectionQueue.remove();
			section.previousFace = sectionQueue.remove();
			section.travelledDirectionMask = sectionQueue.remove();
			section.sectionRef = null;

			//Find Client Chunk Section if Applicable...
			if(section.offsetPosY >= 0 && section.offsetPosY < 16) {
				if(section.sectionRef == null) {
					ClientChunk clientChunk = drawTask.theWorld.requestChunk(
							drawTask.chunkOriginX + section.offsetPosX,
							drawTask.chunkOriginZ + section.offsetPosZ,
							false
					);
					if(clientChunk != null) section.sectionRef = clientChunk.getSectionAt(section.offsetPosY);
				}
			}

			//Update & Queue Draw
			if(section.sectionRef != null) {
				if(section.sectionRef.visibilityNeedsRegen()) {
					section.sectionRef.generateVisibilityMap(OpenVoxel.getInstance().blockRegistry);
				}
				consumer.accept(section.sectionRef);
			}

			//Search all of the nearby directions
			for(int direction = 0; direction < BlockFace.face_count; direction++) {
				int dirX = xOffsets[direction];
				int dirY = yOffsets[direction];
				int dirZ = zOffsets[direction];
				int dirBack = opposite[direction];

				//Check not backwards
				if((section.travelledDirectionMask & (1 << dirBack)) != 0) {
					continue;
				}

				//Check not out of bounds
				int newX = section.offsetPosX + dirX;
				int newY = section.offsetPosY + dirY;
				int newZ = section.offsetPosZ + dirZ;
				if(Math.abs(newX) > viewDistance||
				   Math.abs(newZ) > viewDistance||
				   Math.abs(newY-startOffsetY) > viewDistance) {
					continue;
				}

				//Check Visibility Test
				if(section.previousFace != -1 && section.sectionRef != null) {
					if(!section.sectionRef.isVisible(section.previousFace,direction)) {
						continue;
					}
				}

				//Check not already visited
				if(visitedOffsets.contains(newX,newY,newZ)) {
					continue;
				}

				//Check Frustum Culling
				if(frustum != null) {
					int frustumX = newX * 16;
					int frustumY = newY * 16;
					int frustumZ = newZ * 16;
					if(!frustum.testAab(
							frustumX,
							frustumY,
							frustumZ,
							frustumX + 16,
							frustumY + 16,
							frustumZ + 16
					)) {
						continue;
					}
				}

				//Mark as visited
				visitedOffsets.add(newX,newY,newZ);

				//Add to the queue
				//CullSection cullSection = new CullSection(newX,newY,newZ,opposite[direction],section);
				//cullSection.travelledDirectionMask &= (1 << direction);
				//if(dirY != 0 && section.sectionRef != null && newY >= 0 && newY < 16) {
				//	cullSection.sectionRef = section.sectionRef.getChunk().getSectionAt(newY);
				//}
				sectionQueue.add(newX);
				sectionQueue.add(newY);
				sectionQueue.add(newZ);
				sectionQueue.add(opposite[direction]);
				sectionQueue.add(section.travelledDirectionMask & (1 << direction));
				//sectionQueue.addLast(cullSection);
			}
		}

		//Finish
		sectionQueue.clear();
		visitedOffsets.clear();
	}

	private static class CullSection {
		private ClientChunkSection sectionRef;
		private int offsetPosX;
		private int offsetPosY;
		private int offsetPosZ;
		private int previousFace;
		private int travelledDirectionMask;
		private CullSection(int offsetPosX, int offsetPosY, int offsetPosZ, int previousFace) {
			this.offsetPosX = offsetPosX;
			this.offsetPosY = offsetPosY;
			this.offsetPosZ = offsetPosZ;
			this.previousFace = previousFace;
			this.travelledDirectionMask = 0;
		}
		private CullSection(int offsetPosX, int offsetPosY, int offsetPosZ, int previousFace,CullSection oldValues) {
			this.offsetPosX = offsetPosX;
			this.offsetPosY = offsetPosY;
			this.offsetPosZ = offsetPosZ;
			this.previousFace = previousFace;
			this.travelledDirectionMask = oldValues.travelledDirectionMask;
		}
	}
}
