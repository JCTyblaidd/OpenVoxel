package net.openvoxel.common.item;

import net.openvoxel.common.block.Block;

/**
 * Created by James on 28/08/2016.
 */
public abstract class Item {

	public static Item getItemFromBlock(Block b) {
		return null;//TODO:
	}

	public boolean hasSubTypes() {
		return false;
	}
	public boolean isDamageable() {
		return false;
	}


}
