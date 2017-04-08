package net.openvoxel.vanilla;

import net.openvoxel.common.GameRegistry;
import net.openvoxel.vanilla.block.BlockBricks;

/**
 * Created by James on 24/09/2016.
 *
 * Default Enabled Block Types
 */
public class VanillaBlocks {

	public static BlockBricks BLOCK_BRICKS;

	static void Load() {
		BLOCK_BRICKS = new BlockBricks();
	}

	static void Register() {
		GameRegistry.registerBlock("openvoxel:bricks", BLOCK_BRICKS);
	}

}
