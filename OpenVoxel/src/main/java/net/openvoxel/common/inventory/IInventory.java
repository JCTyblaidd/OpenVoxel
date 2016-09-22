package net.openvoxel.common.inventory;

import com.jc.util.stream.ArrayUtils;
import net.openvoxel.common.item.ItemStack;

/**
 * Created by James on 22/09/2016.
 *
 * Inventory Reference
 */
public interface IInventory {

	int size();

	ItemStack get(int index);

	void set(int index, ItemStack stack);

	default void clear(int index) {
		set(index,null);
	}

	void add(ItemStack stack);

	void clear();

	default void add(ItemStack... stacks) {
		ArrayUtils.Iterate(stacks).forEach(this::add);
	}

}
