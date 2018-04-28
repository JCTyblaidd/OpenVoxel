package net.openvoxel.common;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.gui.menu.ScreenLoading;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.renderer.Renderer;
import net.openvoxel.common.event.init.ModFinalizeInitialisationEvent;
import net.openvoxel.common.event.init.ModInitialisationEvent;
import net.openvoxel.common.event.init.ModPostInitialisationEvent;
import net.openvoxel.common.event.init.ModPreInitialisationEvent;
import net.openvoxel.loader.mods.ModLoader;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 25/08/2016.
 *
 * Utility Class that manages the loading of the game state
 */
public class GameLoader {
	@SideOnly(side=Side.CLIENT)
	public static void LoadGameStateClient(Renderer renderer) {
		Logger gameLogger = Logger.getLogger("Game Loader");
		//Init Code//
		ScreenLoading loadingScreen = new ScreenLoading(6, ModLoader.getInstance().getModCount());
		GUI.addScreen(loadingScreen);
		gameLogger.Info("Starting Mod Loading");
		loadingScreen.startSection("Generate Dependencies");
		ModLoader.getInstance().generateDependencyOrder();
		gameLogger.Info("====Pre Init=====");
		loadingScreen.startSection("Pre Initialization");
		ModLoader.getInstance().propagateInitEvent(new ModPreInitialisationEvent(), "Pre Init", "Sending Pre-Initialisation Event to ", loadingScreen);
		gameLogger.Info("======Init======");
		loadingScreen.startSection("Initialization");
		ModLoader.getInstance().propagateInitEvent(new ModInitialisationEvent(), "Init", "Sending Initialisation Event to ", loadingScreen);
		gameLogger.Info("====Post Init====");
		loadingScreen.startSection("Post Initialization");
		ModLoader.getInstance().propagateInitEvent(new ModPostInitialisationEvent(), "Post Init", "Sending Post-Initialisation Event to ", loadingScreen);
		gameLogger.Info("===Final Init====");
		loadingScreen.startSection("Final Initialization");
		ModLoader.getInstance().propagateInitEvent(new ModFinalizeInitialisationEvent(), "Final Init", "Sending Final-Initialisation Event to ", loadingScreen);
		gameLogger.Info("===Load Textures===");
		loadingScreen.startSection("Texture Loading");
		gameLogger.Info("TODO: FIX BLOCK TEXTURE ATLAS LOADING");
		//OpenVoxel.getInstance().blockRegistry.clientRegisterAll(Renderer.getBlockTextureAtlas());
		gameLogger.Info("Finished Initializing Game State");
		GUI.removeAllScreens();
	}
	@SideOnly(side=Side.DEDICATED_SERVER)
	public static void LoadGameStateServer() {
		Logger gameLogger = Logger.getLogger("Game Loader");
		//Init Code//
		gameLogger.Info("Starting Mod Loading");
		ModLoader.getInstance().generateDependencyOrder();
		gameLogger.Info("====Pre Init=====");
		ModLoader.getInstance().propagateInitEvent(new ModPreInitialisationEvent(), "Pre Init", "Sending Pre-Initialisation Event to ");
		gameLogger.Info("======Init======");
		ModLoader.getInstance().propagateInitEvent(new ModInitialisationEvent(), "Init", "Sending Initialisation Event to ");
		gameLogger.Info("====Post Init====");
		ModLoader.getInstance().propagateInitEvent(new ModPostInitialisationEvent(), "Post Init", "Sending Post-Initialisation Event to ");
		gameLogger.Info("===Final Init====");
		ModLoader.getInstance().propagateInitEvent(new ModFinalizeInitialisationEvent(), "Final Init", "Sending Final-Initialisation Event to ");
		gameLogger.Info("Finished Initializing Game State");
	}
}
