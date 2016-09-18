package net.openvoxel.common;

import net.openvoxel.OpenVoxel;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.entity.Entity;
import net.openvoxel.common.item.Item;
import net.openvoxel.common.world.generation.IWorldGenerator;

/**
 * Created by James on 16/09/2016.
 *
 * Utility Class For Mod Initialisation
 */
public class GameRegistry {

	public static void registerBlock(String ID,Block block) {
		OpenVoxel.getInstance().blockRegistry.registerBlock(ID,block);
	}

	public static void registerItem(String ID, Item item) {
		OpenVoxel.getInstance().itemRegistry.register(ID,item);
	}

	public static void registerEntity(String ID, Class<? extends Entity> entityClass) {
		OpenVoxel.getInstance().entityRegistry.register(ID,entityClass);
	}

	public static void registerWorldGeneratorType(String TYPE, Class<? extends IWorldGenerator> generator) {

	}

}
