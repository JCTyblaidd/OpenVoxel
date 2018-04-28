package net.openvoxel.client.control;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.PerSecondTimer;
import net.openvoxel.client.gui.ScreenDebugInfo;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.old_renderer.generic.GUIRenderer;
import net.openvoxel.client.old_renderer.generic.GlobalRenderer;
import net.openvoxel.client.old_renderer.generic.WorldRenderer;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.world.client.ClientWorld;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 25/08/2016.
 *
 * Render Thread, async from game thread
 */
@Deprecated
public class RenderThread implements Runnable{

	private static RenderThread INSTANCE;
	private Thread thread;
	private PerSecondTimer FPS_TIMER;
	private AtomicBoolean shutdownFlag = new AtomicBoolean(false);
	private AtomicBoolean rThreadRunningFlag = new AtomicBoolean(true);

	public static void Stop() {
		INSTANCE.rThreadRunningFlag.set(false);
	}

	public static void Start() {
		if(INSTANCE == null) {
			INSTANCE = new RenderThread();
			INSTANCE.start();
		}else{
			CrashReport crashReport = new CrashReport("Error With RenderThread::Start").invalidState("Attempted to Start an existing Thread");
			OpenVoxel.reportCrash(crashReport);
		}
	}
	private RenderThread() {
		thread = new Thread(this,"OpenVoxel: Render Thread");
		FPS_TIMER = new PerSecondTimer(64);//Very Volatile Value//
	}

	/**
	 * @return the current frame-rate
	 */
	public static float getFrameRate() {
		return INSTANCE.FPS_TIMER.getPerSecond();
	}

	private void start() {
		thread.start();
	}

	@Override
	public void run() {
		GlobalRenderer renderer = Renderer.renderer;
		renderer.loadPostRenderThread();
		WorldRenderer worldRenderer = renderer.getWorldRenderer();
		GUIRenderer guiRenderer = renderer.getGUIRenderer();
		while(rThreadRunningFlag.get()) {
			//Render World//
			try {
				ClientServer clientServer = OpenVoxel.getClientServer();
				if(clientServer != null) {
					EntityPlayerSP renderTarget = clientServer.getThePlayer();
					if(renderTarget != null && renderTarget.currentWorld != null) {
						worldRenderer.renderWorld(renderTarget,(ClientWorld)renderTarget.currentWorld);
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
				CrashReport crashReport = new CrashReport("Exception Drawing World").caughtException(e);
				OpenVoxel.reportCrash(crashReport);
			}
			//Render GUI//
			try {
				guiRenderer.beginDraw();
				boolean guiDirty = ScreenDebugInfo.debugLevel.get() != ScreenDebugInfo.GUIDebugLevel.NONE;
				synchronized (GUI.class) {
					if(!guiDirty) {
						for (Screen screen : GUI.getStack()) {
							guiDirty |= screen.isDrawDirty();
						}
					}
					if(guiDirty || !guiRenderer.supportDirty()) {
						guiDirty = true;
						GUI.getStack().descendingIterator().forEachRemaining(guiRenderer::DisplayScreen);
					}
				}
				//Debug Screen Renderer//
				guiRenderer.DisplayScreen(ScreenDebugInfo.instance);
				guiRenderer.finishDraw(guiDirty);
			}catch (Exception e) {
				CrashReport crashReport = new CrashReport("Exception Drawing GUI").caughtException(e);
				OpenVoxel.reportCrash(crashReport);
			}
			//Finish//
			renderer.nextFrame();
			FPS_TIMER.notifyEvent();
		}
		Logger.getLogger("Render Thread").Info("Terminating...");
		renderer.kill();
		Logger.getLogger("Render Thread").Info("Terminated");
		shutdownFlag.set(true);
	}

	public static void awaitTermination() {
		for(int i = 0; i < 1000; i++) {
			try{
				Thread.sleep(10);
			}catch(InterruptedException ignored) {}
			if(INSTANCE.shutdownFlag.get()) {
				return;
			}
		}
		Logger.getLogger("Render Thread").Severe("Shutdown Failed");
	}
}
