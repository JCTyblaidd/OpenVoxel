package net.openvoxel.common;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.util.PerSecondTimer;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.gui.menu.ScreenMainMenu;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.common.event.init.ModFinalizeInitialisationEvent;
import net.openvoxel.common.event.init.ModInitialisationEvent;
import net.openvoxel.common.event.init.ModPostInitialisationEvent;
import net.openvoxel.common.event.init.ModPreInitialisationEvent;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.server.Server;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 25/08/2016.
 *
 * Thread That Handles Game Logic
 */
public class GameThread implements Runnable{

	public static GameThread INSTANCE;
	private Thread thread;
	private OpenVoxel openVoxel;
	private PerSecondTimer tickTimer;
	private Logger gameLogger = Logger.getLogger("Game Thread");
	private AtomicBoolean hasLoadedMods = new AtomicBoolean(false);

	public static void Start() {
		INSTANCE = new GameThread();
		INSTANCE.thread.start();
	}

	public GameThread() {
		thread = new Thread(this,"OpenVoxel: Game Logic Thread");
		openVoxel = OpenVoxel.getInstance();
		tickTimer = new PerSecondTimer();
	}

	private float getTicksPerSecond() {
		return tickTimer.getPerSecond();
	}

	private void initMods() {
		//debug//
		gameLogger.Info("Starting Mod Loading");
		ModLoader.getInstance().generateDependencyOrder();
		gameLogger.Info("====Pre Init=====");
		ModLoader.getInstance().propagateInitEvent(new ModPreInitialisationEvent(),"Pre Init","Sending Pre-Initialisation Event to ");
		gameLogger.Info("======Init======");
		ModLoader.getInstance().propagateInitEvent(new ModInitialisationEvent(),"Init","Sending Initialisation Event to ");
		gameLogger.Info("====Post Init====");
		ModLoader.getInstance().propagateInitEvent(new ModPostInitialisationEvent(),"Post Init","Sending Post-Initialisation Event to ");
		gameLogger.Info("===Final Init====");
		ModLoader.getInstance().propagateInitEvent(new ModFinalizeInitialisationEvent(),"Final Init","Sending Final-Initialisation Event to ");;
		hasLoadedMods.set(true);
	}

	public void awaitModsLoaded() {
		while(!hasLoadedMods.get()) {
			Thread.yield();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}


	private void initSide() {
		if(Side.isClient) {
			gameLogger.Info("Starting Texture Stitching");
			Renderer.getBlockTextureAtlas().performStitch();
			gameLogger.Info("Finished");
			GUI.removeAllScreens();
			GUI.addScreen(new ScreenMainMenu());
		}else{
			gameLogger.Info("Starting Server Hosting");
		}
	}

	private void runGameLogic() {
		Server server = OpenVoxel.getServer();
		if(server != null) {
			server.gameLogicTick();
		}
	}

	private static final int TICK_DELAY = 50;//1000 / 20;

	@Override
	public void run() {
		initMods();
		initSide();
		long last_time = System.currentTimeMillis();
		long current_time, time_taken, wait_time;
		while(openVoxel.isRunning) {
			//Run Game Logic//
			try{
				runGameLogic();
			}catch(Exception e) {
				e.printStackTrace();
			}
			current_time = tickTimer.notifyEvent();
			time_taken = last_time - current_time;
			if(time_taken < TICK_DELAY) {
				//The Thread Must Sleep//
				wait_time = TICK_DELAY - time_taken;
				try {
					Thread.sleep(wait_time);
				}catch(InterruptedException e) {}
			}
			last_time = System.currentTimeMillis();
		}
	}
}
