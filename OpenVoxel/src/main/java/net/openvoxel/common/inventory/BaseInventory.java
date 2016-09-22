package net.openvoxel.common.inventory;

import com.jc.util.stream.Range;
import net.openvoxel.common.item.ItemStack;

/**
 * Created by James on 22/09/2016.
 *
 * Generic Inventory Implementation
 */
public abstract class BaseInventory implements IInventory {

	private final int size;
	private final ItemStack[] Data;

	public BaseInventory(int size) {
		this.size = size;
		Data = new ItemStack[size];
	}

	@Override
	public void clear() {
		Range.in(size).call(this::clear);
	}

	@Override
	public void set(int index, ItemStack stack) {
		Data[index] = stack;
	}

	@Override
	public ItemStack get(int index) {
		return Data[index];
	}

	@Override
	public void add(ItemStack stack) {

	}

	@Override
	public int size() {
		return size;
	}
}
