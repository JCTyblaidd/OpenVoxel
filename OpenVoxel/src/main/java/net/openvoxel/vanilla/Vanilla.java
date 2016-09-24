package net.openvoxel.vanilla;

import net.openvoxel.api.mods.Mod;
import net.openvoxel.api.mods.ModInitEventHandler;
import net.openvoxel.api.mods.ModInstance;
import net.openvoxel.common.GameRegistry;
import net.openvoxel.common.event.init.ModFinalizeInitialisationEvent;
import net.openvoxel.common.event.init.ModInitialisationEvent;
import net.openvoxel.common.event.init.ModPostInitialisationEvent;
import net.openvoxel.common.event.init.ModPreInitialisationEvent;
import net.openvoxel.vanilla.block.BlockBricks;

/**
 * Created by James on 25/08/2016.
 *
 * The Default Content Mod
 */
@SuppressWarnings("unused")
@Mod(id = "vanilla", requiredMods = {}, loadAfter = {}, name = "OpenVoxel Vanilla", minimumOpenVoxelVersion = "0.0.1-Alpha", version = "0.0.1-Alpha")
public class Vanilla {

	@ModInstance
	public static Vanilla INSTANCE;

	@ModInitEventHandler
	public void preInit(ModPreInitialisationEvent e) {
		VanillaBlocks.Load();
		VanillaBlocks.Register();
	}

	@ModInitEventHandler
	public void Init(ModInitialisationEvent e) {

	}

	@ModInitEventHandler
	public void postInit(ModPostInitialisationEvent e) {

	}

	@ModInitEventHandler
	public void finalInit(ModFinalizeInitialisationEvent e) {

	}


}
