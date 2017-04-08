package net.openvoxel.common.world.generation;

import net.openvoxel.OpenVoxel;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.ChunkCoordinate;
import net.openvoxel.vanilla.VanillaBlocks;

/**
 * Created by James on 04/09/2016.
 *
 * Debug Annoying World Generator
 */
public class DebugWorldGenerator implements IWorldGenerator{

	@Override
	public Chunk generateChunk(ChunkCoordinate coordinate) {
		Chunk chunk = new Chunk(coordinate);
		int blockID = OpenVoxel.getInstance().blockRegistry.getIDFromBlock(VanillaBlocks.BLOCK_BRICKS);
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 0; y < 100; y++) {
					chunk.setBlockAt(x,y,z,blockID,(byte)0);//Debug Data
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
