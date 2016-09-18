package net.openvoxel.common.registry;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.common.item.Item;

import java.util.HashMap;

/**
 * Created by James on 28/08/2016.
 *
 * TODO:
 */
public class RegistryItems {

	private RegistryBlocks linked_registry;
	private TIntObjectHashMap<Item> itemMap;
	private TObjectIntHashMap<Item> reverseMap;
	private HashMap<String,Item> idMap;

	public RegistryItems(RegistryBlocks linked) {
		linked_registry = linked;
	}

	public void register(String ID, Item item) {

	}
}
