package net.openvoxel.common.event.init;

import net.openvoxel.loader.mods.ModLoader;

/**
 * Created by James on 25/08/2016.
 *
 * Late Initialisation Event:
 *      - Send Cross Mod Recipes:
 */
public class ModPostInitialisationEvent extends ModInitEvent{

	public boolean modExists(String modID) {
		return ModLoader.isModLoaded(modID);
	}

	public void sendMessage(String modID,String messageID,Object message) {
		ModLoader.sendModMessage(modID,messageID,message);
	}

}
