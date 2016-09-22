package net.openvoxel.common.item;

import net.openvoxel.common.block.Block;

/**
 * Created by James on 22/09/2016.
 *
 * Item Implementation Reference
 *
 * Item Type, Item Number, Item Damage
 *
 */
public class ItemStack {

	public int damage;
	public Item item;
	public int stackSize;

	public ItemStack(Block b) {
		this(b,1);
	}
	public ItemStack(Block b, int stackSize) {
		this(b,stackSize,1);
	}
	public ItemStack(Block b, int stackSize, int damage) {
		this(Item.getItemFromBlock(b),stackSize,damage);
	}

	public ItemStack(Item i) {
		this(i,1);
	}
	public ItemStack(Item i, int stackSize) {
		this(i,stackSize,0);
	}
	public ItemStack(Item i, int stackSize,int damage) {
		this.item = i;
		this.stackSize = stackSize;
		this.damage = damage;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(getClass().equals(obj.getClass())) {
			ItemStack stack = (ItemStack)obj;
			return stack.item.equals(item) && stack.damage == damage && stack.stackSize == stackSize;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return item.hashCode() ^ damage;
	}
}
