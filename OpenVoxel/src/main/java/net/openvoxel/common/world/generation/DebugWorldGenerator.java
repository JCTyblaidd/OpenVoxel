package net.openvoxel.common.world.generation;

import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.world.chunk.Chunk;
import net.openvoxel.vanilla.VanillaBlocks;

/**
 * Created by James on 04/09/2016.
 *
 * Debug Annoying World Generator
 */
public class DebugWorldGenerator implements IWorldGenerator{

	@Override
	public Chunk generateChunk(int xv, int zv) {
		Chunk chunk = new Chunk(xv,zv);
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 0; y < 100; y++) {
					chunk.setBlock(x,y,z,VanillaBlocks.BLOCK_BRICKS,(byte)0);
				}
			}
		}
		return null;
	}

	@Override
	public ResourceHandle getSkyMapResource() {
		return null;
	}

}
