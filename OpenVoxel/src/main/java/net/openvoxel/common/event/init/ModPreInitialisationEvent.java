package net.openvoxel.common.event.init;

import net.openvoxel.OpenVoxel;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.entity.Entity;
import net.openvoxel.common.item.Item;

/**
 * Created by James on 25/08/2016.
 *
 * Pre Initialisation:
 *  -Register Blocks
 *  -Register Items
 *  -Register Entities
 */
public class ModPreInitialisationEvent extends ModInitEvent{

	public final Block registerBlock(String ID,Block block) {
		OpenVoxel.getInstance().blockRegistry.registerBlock(ID,block);
		return block;
	}

	public final void registerItem(String ID,Item item) {
		//OpenVoxel.getInstance().itemRegistry;
	}

	public final void registerEntity(String ID, Class<? extends Entity> type) {
		//
	}

}
