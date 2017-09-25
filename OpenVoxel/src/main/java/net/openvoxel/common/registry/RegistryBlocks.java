package net.openvoxel.common.registry;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.BlockAir;
import net.openvoxel.utility.CrashReport;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 28/08/2016.
 *
 * Block Registry
 */
public class RegistryBlocks {

	private TIntObjectHashMap<Block> id_map;
	private TObjectIntHashMap<Block> reverse_id_map;
	private HashMap<String,Block> block_map;
	private HashMap<Block,String> name_map;

	//For Generation//
	private Map<Block,Integer> fixed_id_map;

	public RegistryBlocks() {
		id_map = new TIntObjectHashMap<>();
		reverse_id_map = new TObjectIntHashMap<>();
		block_map = new HashMap<>();
		name_map = new HashMap<>();
		fixed_id_map = new HashMap<>();
		//Register AIR At Default ID
		registerBlockWithFixedID(0,"openvoxel:air",BlockAir.BLOCK_AIR);
	}

	public void registerBlock(String ID, Block block) {
		block_map.put(ID,block);
		name_map.put(block,ID);
	}

	/**
	 * To Be Used In Rare Circumstances: Expected Only Use: Registry AIR
	 */
	private void registerBlockWithFixedID(int fixedID,String ID, Block block) {
		registerBlock(ID,block);
		fixed_id_map.put(block,fixedID);
	}

	public int getIDFromBlock(Block block) {
		return reverse_id_map.get(block);
	}
	public int getIDFromName(String name) {
		return getIDFromBlock(getBlockFromName(name));
	}
	public Block getBlockFromID(int ID) {
		Block res =  id_map.get(ID);
		if(res == null) {
			res =  BlockAir.BLOCK_AIR;
		}
		return res;
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
		id_map.clear();
		reverse_id_map.clear();
		fixed_id_map.forEach((block,id) -> {
			id_map.put(id,block);
			reverse_id_map.put(block,id);
		});
		int current_id = 0;
		for(Map.Entry<Block,String> entry : name_map.entrySet()) {
			if(!fixed_id_map.containsKey(entry.getKey())) {
				while(id_map.containsKey(current_id)) {
					current_id++;
				}
				id_map.put(current_id,entry.getKey());
				reverse_id_map.put(entry.getKey(),current_id);
			}
		}
	}

	public TObjectIntMap<String> getDataMap() {
		TObjectIntMap<String> required = new TObjectIntHashMap<>();
		block_map.keySet().forEach(k -> required.put(k,getIDFromName(k)));
		return required;
	}

	public void generateMappingsFromPrevious(TObjectIntMap<String> previousMap) {
		id_map.clear();
		reverse_id_map.clear();
		fixed_id_map.forEach((block,id) -> {
			id_map.put(id,block);
			reverse_id_map.put(block,id);
		});
		previousMap.forEachEntry((name,id) -> {
			Block b = block_map.get(name);
			if(b == null) {
				OpenVoxel.reportCrash(new CrashReport("Block Doesn't Exist")
						                      .invalidState("Previous Block Name Not Found!!!")
						                      .unexpectedNull("block_map.get(name);"));
			}
			id_map.put(id,b);
			reverse_id_map.put(b,id);
			return true;
		});
	}

	@SideOnly(side= Side.CLIENT)
	public void clientRegisterAll(IconAtlas atlas) {
		name_map.keySet().forEach(o -> o.loadTextureAtlasData(atlas));
	}

}
