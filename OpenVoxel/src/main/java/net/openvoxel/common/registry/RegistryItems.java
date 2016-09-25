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
	private HashMap<Item,String> registerMap;

	public RegistryItems(RegistryBlocks linked) {
		linked_registry = linked;
		itemMap = new TIntObjectHashMap<>();
		reverseMap = new TObjectIntHashMap<>();
		idMap = new HashMap<>();
		registerMap = new HashMap<>();
	}

	public void register(String ID, Item item) {

	}
}
