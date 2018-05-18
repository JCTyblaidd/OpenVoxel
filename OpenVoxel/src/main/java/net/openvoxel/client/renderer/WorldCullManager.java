package net.openvoxel.client.renderer;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.utility.collection.trove_extended.TVec3LHashSet;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class WorldCullManager {

	private final WorldDrawTask drawTask;

	WorldCullManager(WorldDrawTask drawTask) {
		this.drawTask = drawTask;
	}

	/*
	 * Return if the chunk is in the view frustum
	 */
	private boolean cullChunkFrustum(float sectionX, float sectionY, float sectionZ) {
		return drawTask.frustumIntersect.testAab(
				sectionX,
				sectionY,
				sectionZ,
				sectionX + 16.F,
				sectionY + 16.F,
				sectionZ + 16.F
		);
	}


	void runFrustumCull(Consumer<ClientChunkSection> consumer) {
		Deque<CullSection> sectionQueue = new ArrayDeque<>();
		TVec3LHashSet visitedOffsets = new TVec3LHashSet();

		//Find Starting Chunk offset Position
		int startOffsetX = (int)Math.floor(drawTask.playerX / 16.0);
		int startOffsetY = (int)Math.floor(drawTask.playerY / 16.0);//TODO: ADD CAMERA OFFSET!!!!!!!!
		int startOffsetZ = (int)Math.floor(drawTask.playerZ / 16.0);

		//Add Starting Chunk
		sectionQueue.add(new CullSection(startOffsetX,startOffsetY,startOffsetZ,-1));

		//Constants
		final int[] xOffsets = BlockFace.array_xOffsets;
		final int[] yOffsets = BlockFace.array_yOffsets;
		final int[] zOffsets = BlockFace.array_zOffsets;
		final int[] opposite = BlockFace.array_opposite;

		//Breath First Search
		while(!sectionQueue.isEmpty()) {
			//System.out.println("FROM SIZE="+sectionQueue.size());
			CullSection section = sectionQueue.removeFirst();

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
					section.sectionRef.generateVisibilityMap();
				}
				consumer.accept(section.sectionRef);
			}

			//Search all of the nearby directions
			for(int direction = 0; direction < BlockFace.face_count; direction++) {
				int dirX = xOffsets[direction];
				int dirY = yOffsets[direction];
				int dirZ = zOffsets[direction];

				//Check not backwards
				float dotProduct = drawTask.cameraVector.dot(dirX,dirY,dirZ);
				if(dotProduct > 0.0F) {
					//System.out.println("Invalidate Dot Product");
					//System.out.println("INVALIDATE DOT PRODUCT! {"+
					//		                   drawTask.cameraVector.x+","+drawTask.cameraVector.y+","+drawTask.cameraVector.z+
					//		                   "|"+dirX+","+dirY+","+dirZ+"}");
					continue;
				}

				//Check not out of bounds
				int newX = section.offsetPosX + dirX;
				int newY = section.offsetPosY + dirY;
				int newZ = section.offsetPosZ + dirZ;
				if(Math.abs(newX) > drawTask.viewDistance||Math.abs(newZ) > drawTask.viewDistance
				   ||Math.abs(newY-startOffsetY) > drawTask.viewDistance) {
					//System.out.println("Invalidate View Distance {"+newX+","+newZ+"}");
					continue;
				}

				//Check Visibility Test
				if(section.previousFace != -1 && section.sectionRef != null) {
					if(!section.sectionRef.isVisible(section.previousFace,direction)) {
						//System.out.println("Invalidate Invisibility");
						continue;
					}
				}

				//Check not already visited
				if(visitedOffsets.contains(newX,newY,newZ)) {
					//System.out.println("Invalidate Visited");
					continue;
				}

				//Check Frustum Culling
				if(!cullChunkFrustum(newX * 1.6F, newY * 16.F, newZ * 16.F)) {
					//System.out.println("Invalidate Frustum");
					continue;
				}

				//System.out.println("Validated!");

				//Queue for visitation
				CullSection cullSection = new CullSection(newX,newY,newZ,opposite[direction]);
				if(dirY != 0 && section.sectionRef != null) {
					cullSection.sectionRef = section.sectionRef.getChunk().getSectionAt(newY);
				}
				sectionQueue.addLast(cullSection);
			}

			//Finished - add to set
			visitedOffsets.add(section.offsetPosX,section.offsetPosY,section.offsetPosZ);
		}
	}

	private static class CullSection {
		private ClientChunkSection sectionRef;
		private int offsetPosX;
		private int offsetPosY;
		private int offsetPosZ;
		private int previousFace;
		private CullSection(int offsetPosX, int offsetPosY, int offsetPosZ, int previousFace) {
			this.offsetPosX = offsetPosX;
			this.offsetPosY = offsetPosY;
			this.offsetPosZ = offsetPosZ;
			this.previousFace = previousFace;
		}
	}
}
