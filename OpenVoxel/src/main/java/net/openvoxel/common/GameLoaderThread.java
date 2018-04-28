package net.openvoxel.common;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.side.Side;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.gui.menu.ScreenLoading;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.common.event.init.ModFinalizeInitialisationEvent;
import net.openvoxel.common.event.init.ModInitialisationEvent;
import net.openvoxel.common.event.init.ModPostInitialisationEvent;
import net.openvoxel.common.event.init.ModPreInitialisationEvent;
import net.openvoxel.loader.mods.ModLoader;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 25/08/2016.
 *
 * Thread That Handles Asynchronously Initializing Game Logic
 */
public class GameLoaderThread implements Runnable{

	private static GameLoaderThread INSTANCE;
	private Thread thread;
	private Logger gameLogger = Logger.getLogger("Game Loader");
	private AtomicBoolean hasLoadedMods = new AtomicBoolean(false);

	public static void StartLoad() {
		INSTANCE = new GameLoaderThread();
		INSTANCE.thread.start();
	}

	public static void AwaitLoadFinish() {
		INSTANCE.awaitModsLoaded();
		INSTANCE = null;
	}

	private GameLoaderThread() {
		thread = new Thread(this,"OpenVoxel: Game Loader Thread");
	}

	@Override
	public void run() {
		if(Side.isClient) {
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
		}else{
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
		hasLoadedMods.set(true);
	}

	private void awaitModsLoaded() {
		while(!hasLoadedMods.get()) {
			Thread.yield();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {}
		}
	}
}
