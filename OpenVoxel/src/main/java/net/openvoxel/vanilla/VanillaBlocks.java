package net.openvoxel.vanilla;

import net.openvoxel.common.GameRegistry;
import net.openvoxel.vanilla.block.BlockBricks;
import net.openvoxel.vanilla.block.BlockDirt;
import net.openvoxel.vanilla.block.BlockGrass;
import net.openvoxel.vanilla.block.BlockWater;

/**
 * Created by James on 24/09/2016.
 *
 * Default Enabled Block Types
 */
public class VanillaBlocks {

	public static BlockBricks BLOCK_BRICKS;
	public static BlockDirt BLOCK_DIRT;
	public static BlockGrass BLOCK_GRASS;
	public static BlockWater BLOCK_WATER;

	static void Load() {
		BLOCK_BRICKS = new BlockBricks();
		BLOCK_DIRT = new BlockDirt();
		//BLOCK_GRASS = new BlockGrass();
		BLOCK_WATER = new BlockWater();
	}

	static void Register() {
		GameRegistry.registerBlock("openvoxel:bricks", BLOCK_BRICKS);
		GameRegistry.registerBlock("openvoxel:dirt",BLOCK_DIRT);
		//GameRegistry.registerBlock("openvoxel:grass",BLOCK_GRASS);
		GameRegistry.registerBlock("openvoxel:water",BLOCK_WATER);
	}

}
