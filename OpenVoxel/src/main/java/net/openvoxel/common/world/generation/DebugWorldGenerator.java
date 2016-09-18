package net.openvoxel.common.world.generation;

import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.ChunkCoordinate;

/**
 * Created by James on 04/09/2016.
 */
public class DebugWorldGenerator implements IWorldGenerator{

	@Override
	public Chunk generateChunk(ChunkCoordinate coordinate) {
		Chunk chunk = new Chunk(coordinate);
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 0; y < 100; y++) {
					chunk.setBlockAt(x,y,z,1,(byte)0);//Debug Data
				}
			}
		}
		return null;
	}

	@Override
	public ResourceHandle getSkymapResource() {
		return null;
	}

}
