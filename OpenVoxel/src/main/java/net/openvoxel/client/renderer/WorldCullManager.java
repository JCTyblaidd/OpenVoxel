package net.openvoxel.client.renderer;

import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;

import java.util.function.Consumer;

public class WorldCullManager {

	private final int cullX;
	private final int cullY;
	private final int cullZ;
	private final int divide;
	private WorldDrawTask drawTask;
	private Consumer<ClientChunkSection> consumer;

	WorldCullManager(int x, int y, int z, int d) {
		cullX = x;
		cullY = y;
		cullZ = z;
		divide = d;
	}

	public void runCull(int viewDistance, WorldDrawTask drawTask, Consumer<ClientChunkSection> consumer) {
		//Init	protected
		this.drawTask = drawTask;
		this.consumer = consumer;

		//Calculate Initial State
		int cullSize = (viewDistance * 2)/ divide;
		int minX = cullX * cullSize;
		int maxX = minX + cullSize;
		int minZ = cullZ * cullSize;
		int maxZ = minZ + cullSize;
		int minY = cullY * 8;
		int maxY = minY + 8;

		//Run the culling code
		runCull(minX,maxX,minY,maxY,minZ,maxZ);
	}

	private void runCull(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		boolean success = drawTask.frustumIntersect.testAab(
				minX * 16.0F,
				minY * 16.0F,
				minZ * 16.0F,
				maxX * 16.0F,
				maxY * 16.0F,
				maxZ * 16.0F
		);
		//TODO: REMOVE & FIX
		success = true;
		if(success) {
			if(minX + 1 == maxX && minY + 1 == maxY && minZ + 1 == maxZ) {
				ClientChunk chunk = drawTask.theWorld.requestChunk(
						minX + drawTask.chunkOriginX,
						minZ + drawTask.chunkOriginZ,
						false
				);
				if(chunk != null) {
					ClientChunkSection section = chunk.getSectionAt(minY);
					consumer.accept(section);
				}
			}else{
				int splitX = ((maxX - minX) / 2) + minX;
				int splitY = ((maxY - minY) / 2) + minY;
				int splitZ = ((maxZ - minZ) / 2) + minZ;
				int[] xSplit = new int[]{minX,splitX,maxX};
				int[] ySplit = new int[]{minY,splitY,maxY};
				int[] zSplit = new int[]{minZ,splitZ,maxZ};
				int x_start = splitX != minX ? 0 : 1;
				int y_start = splitY != minY ? 0 : 1;
				int z_start = splitZ != minZ ? 0 : 1;
				for(int xi = x_start; xi < 2; xi++) {
					int min_x = xSplit[xi];
					int max_x = xSplit[xi+1];
					for(int yi = y_start; yi < 2; yi++) {
						int min_y = ySplit[yi];
						int max_y = ySplit[yi+1];
						for(int zi = z_start; zi < 2; zi++) {
							int min_z = zSplit[zi];
							int max_z = zSplit[zi+1];
							runCull(min_x,max_x,min_y,max_y,min_z,max_z);
						}
					}
				}
			}
		}
	}

}
