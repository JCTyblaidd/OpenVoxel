package net.openvoxel.world.generation;

import net.openvoxel.common.block.BlockAir;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.vanilla.VanillaBlocks;
import net.openvoxel.world.chunk.Chunk;
import net.openvoxel.world.client.ClientChunk;

/**
 * Created by James on 04/09/2016.
 *
 * Debug Annoying World Generator
 */
public class DebugWorldGenerator implements IWorldGenerator{

	@Override
	public Chunk generateChunk(int xv, int zv) {
		ClientChunk chunk = new ClientChunk(xv,zv);
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 0; y < 100; y++) {
					chunk.setBlock(x,y,z,VanillaBlocks.BLOCK_BRICKS,(byte)0);
				}
				for(int y = 100; y < 256; y++) {
					chunk.setBlock(x,y,z, BlockAir.BLOCK_AIR,(byte)0);
				}
			}
		}
		for(int x = 3; x < 4; x++) {
			for(int z = 1; z < 9; z++) {
				chunk.setBlock(x,99,z,VanillaBlocks.BLOCK_WATER,(byte)0);
			}
		}
		chunk.setBlock(3,99,8,VanillaBlocks.BLOCK_DIRT,(byte)0);

		for(int x = 9; x < 14; x++) {
			for(int z = 4; z < 14; z++) {
				chunk.setBlock(x,101,z,VanillaBlocks.BLOCK_DIRT,(byte)0);
			}
		}
		for(int y = 100; y < 125; y++) {
			chunk.setBlock(8,y,8,VanillaBlocks.BLOCK_BRICKS,(byte)0);
		}
		return chunk;
	}

	@Override
	public ResourceHandle getSkyMapResource() {
		return null;
	}

}
