package net.openvoxel.common;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.gui.framework.GUI;
import net.openvoxel.client.gui.menu.ScreenLoading;
import net.openvoxel.client.renderer.Renderer;
import net.openvoxel.common.event.init.*;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.utility.async.AsyncBarrier;

/**
 * Created by James on 25/08/2016.
 *
 * Utility Class that manages the loading of the game state
 */
public class GameLoader {

	@SideOnly(side=Side.DEDICATED_SERVER)
	public static void LoadGameStateServer() {
		GameLoader serverLoader = new GameLoader(null);
		serverLoader.callFunctionRaw("Generate Dependencies",ModLoader.getInstance()::generateDependencyOrder);
		serverLoader.callEventRaw("Pre-Init","Pre-Initialization",new ModPreInitialisationEvent());
		serverLoader.callEventRaw("Init","Initialization",new ModInitialisationEvent());
		serverLoader.callEventRaw("Post-Init","Post-Initialization",new ModPostInitialisationEvent());
		serverLoader.callEventRaw("Final-Init","Final-Initialization",new ModFinalizeInitialisationEvent());
		serverLoader.finishAllRaw();
	}

	@SideOnly(side=Side.CLIENT)
	public static void LoadGameStateClient(Renderer renderer) {
		GameLoader clientLoader = new GameLoader(renderer);
		clientLoader.callFunction("Generate Dependencies", ModLoader.getInstance()::generateDependencyOrder);
		clientLoader.callEvent("Pre-Init","Pre-Initialization",new ModPreInitialisationEvent());
		clientLoader.callEvent("Init","Initialization",new ModInitialisationEvent());
		clientLoader.callEvent("Post-Init","Post-Initialization",new ModPostInitialisationEvent());
		clientLoader.callEvent("Final-Init","Final-Initialization",new ModFinalizeInitialisationEvent());
		clientLoader.callFunction("Texture Loading",
				() -> OpenVoxel.getInstance().blockRegistry.clientRegisterAll(renderer.getBlockAtlas())
		);
		clientLoader.finishAll();
	}

	///
	/// Client Side Implementation
	///

	@SideOnly(side = Side.CLIENT)
	private Renderer renderer;
	@SideOnly(side = Side.CLIENT)
	private ScreenLoading loading;

	private Logger gameLogger;

	private GameLoader(Renderer renderer) {
		if(renderer != null) _loadClient(renderer);
		gameLogger = Logger.getLogger("Game Loader");
		gameLogger.Info("Starting");
	}

	@SideOnly(side = Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void _loadClient(Renderer renderer) {
		this.renderer = renderer;
		this.loading = new ScreenLoading(6,ModLoader.getInstance().getModCount());
		GUI.addScreen(loading);
	}

	/*
	 * Runs 1 cycle of the draw loop (to update the screen!)
	 */
	@SideOnly(side = Side.CLIENT)
	private void updateScreen() {
		AsyncBarrier barrier = new AsyncBarrier();
		renderer.pollInputs();
		renderer.prepareFrame();
		renderer.generateUpdatedChunks(null,barrier);
		barrier.awaitCompletion();
		renderer.startAsyncGUIDraw(barrier);
		barrier.awaitCompletion();
		renderer.submitFrame(barrier);
		barrier.awaitCompletion();
	}

	@SideOnly(side=Side.CLIENT)
	private void startSection(String name) {
		loading.startSection(name);
		updateScreen();
	}

	@SideOnly(side=Side.CLIENT)
	private void finishSection() {
		updateScreen();
	}

	@SideOnly(side=Side.CLIENT)
	public void startMod(String name) {
		loading.startMod(name);
		updateScreen();
	}

	//
	// Client Only API
	//


	@SideOnly(side=Side.CLIENT)
	private void callFunction(String name,Runnable target) {
		startSection(name);
		callFunctionRaw(name,target);
		finishSection();
	}

	@SideOnly(side=Side.CLIENT)
	private void callEvent(String shortName,String longName, ModInitEvent event) {
		startSection(longName);
		callEventRaw(shortName,longName,event);
		finishSection();
	}

	@SideOnly(side=Side.CLIENT)
	private void finishAll() {
		finishAllRaw();
		GUI.removeAllScreens();
	}


	//
	// Common Code API
	//

	private void callFunctionRaw(String name,Runnable target) {
		Logger sectionLogger = gameLogger.getSubLogger(name);
		sectionLogger.Info("Starting");
		target.run();
		sectionLogger.Info("Finished");
	}

	private void callEventRaw(String shortName,String longName, ModInitEvent event) {
		Logger sectionLogger = gameLogger.getSubLogger(shortName);
		sectionLogger.Info("Starting");
		ModLoader.getInstance().propagateInitEvent(event,
				shortName,
				"Sending " + longName + " Event to ",
				this
		);
		sectionLogger.Info("Finished");
	}

	private void finishAllRaw() {
		gameLogger.Info("Finished");
	}

}
