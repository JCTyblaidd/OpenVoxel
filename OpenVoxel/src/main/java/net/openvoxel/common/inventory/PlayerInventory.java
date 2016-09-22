package net.openvoxel.common.inventory;

import net.openvoxel.common.item.ItemStack;

/**
 * Created by James on 22/09/2016.
 *
 * Players Inventory
 */
public class PlayerInventory extends BaseInventory{

	public ItemStack handItem;

	public PlayerInventory() {
		super(10 * 5);
	}

	public void setItemInHand(ItemStack stack) {
		handItem = stack;
	}

}
