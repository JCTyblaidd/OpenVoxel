package net.openvoxel.common.registry;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.common.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 28/08/2016.
 */
public class RegistryBlocks {

	private TIntObjectHashMap<Block> id_map;
	private TObjectIntHashMap<Block> reverse_id_map;
	private HashMap<String,Block> block_map;
	private HashMap<Block,String> name_map;

	//For Generation//
	private Map<Block,Integer> fixed_id_map;

	public void registerBlock(String ID, Block block) {
		block_map.put(ID,block);
		name_map.put(block,ID);
	}

	/**
	 * To Be Used In Rare Circumstances: Expected Only Use: Registry AIR
	 */
	public void registerBlockWithFixedID(int fixedID,String ID, Block block) {
		registerBlock(ID,block);
		fixed_id_map.put(block,fixedID);
	}

	public Integer getIDFromBlock(Block block) {
		return reverse_id_map.get(block);
	}
	public Integer getIDFromName(String name) {
		return getIDFromBlock(getBlockFromName(name));
	}
	public Block getBlockFromID(int ID) {
		return id_map.get(ID);
	}
	public Block getBlockFromName(String name) {
		return block_map.get(name);
	}
	public String getNameFromBlock(Block block) {
		return name_map.get(block);
	}
	public String getNameFromID(int ID) {
		return getNameFromBlock(getBlockFromID(ID));
	}

	public void generateMappingsFromRaw() {

	}
	public void generateMappingsFromPrevious(HashMap<String,Integer> previousMap) {

	}

}
