package net.openvoxel.vanilla;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.mods.Mod;
import net.openvoxel.api.mods.ModInitEventHandler;
import net.openvoxel.api.mods.ModInstance;
import net.openvoxel.api.mods.ModLogger;
import net.openvoxel.common.event.init.ModFinalizeInitialisationEvent;
import net.openvoxel.common.event.init.ModInitialisationEvent;
import net.openvoxel.common.event.init.ModPostInitialisationEvent;
import net.openvoxel.common.event.init.ModPreInitialisationEvent;

/**
 * Created by James on 25/08/2016.
 *
 * The Default Content Mod
 */
@SuppressWarnings("unused")
@Mod(id = "vanilla", requiredMods = {}, loadAfter = {}, name = "OpenVoxel Vanilla", minimumOpenVoxelVersion = "0.0.1-Alpha", version = "0.0.2-Alpha")
public class Vanilla {

	@ModInstance
	public static Vanilla INSTANCE;

	@ModLogger
	private static Logger vanillaLogger;

	@ModInitEventHandler
	public void preInit(ModPreInitialisationEvent e) {
		VanillaBlocks.Load();
		vanillaLogger.Info("Loaded Vanilla Blocks");
		vanillaLogger.Info("Loaded Vanilla Items");
		vanillaLogger.Info("Loaded Vanilla Entities");
		VanillaBlocks.Register();
		vanillaLogger.Info("Registered Vanilla Blocks");
		vanillaLogger.Info("Registered Vanilla Items");
		vanillaLogger.Info("Registered Vanilla Entities");
		simulateSlowLoad();
	}

	@ModInitEventHandler
	public void Init(ModInitialisationEvent e) {
		simulateSlowLoad();
		vanillaLogger.Info("Loaded Vanilla Dimensions");
		vanillaLogger.Info("Registered Vanilla Dimensions");
		vanillaLogger.Info("Registered Vanilla Packets");
	}

	@ModInitEventHandler
	public void postInit(ModPostInitialisationEvent e) {
		simulateSlowLoad();
		vanillaLogger.Info("Registered Vanilla Recipes");
	}

	@ModInitEventHandler
	public void finalInit(ModFinalizeInitialisationEvent e) {
		simulateSlowLoad();
	}

	private void simulateSlowLoad() {
		if(OpenVoxel.getLaunchParameters().hasFlag("simulateSlowLoad")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
