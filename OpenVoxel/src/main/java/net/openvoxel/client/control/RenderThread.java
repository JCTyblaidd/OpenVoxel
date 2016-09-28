package net.openvoxel.client.control;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.util.PerSecondTimer;
import net.openvoxel.client.gui.ScreenDebugInfo;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.common.world.World;
import net.openvoxel.utility.CrashReport;

/**
 * Created by James on 25/08/2016.
 *
 * Render Thread, async from game thread
 */
public class RenderThread implements Runnable{

	private static RenderThread INSTANCE;
	private Thread thread;
	private PerSecondTimer FPS_TIMER;

	public static void Start() {
		if(INSTANCE != null) {
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
		thread.setPriority(Thread.MAX_PRIORITY);//EXTREMELY IMPORTANT
	}

	/**
	 * @return the current frame-rate
	 */
	public static float getFrameRate() {
		return INSTANCE.FPS_TIMER.getPerSecond();
	}

	void start() {
		thread.start();
	}

	@Override
	public void run() {
		OpenVoxel inst = OpenVoxel.getInstance();
		GlobalRenderer renderer = Renderer.renderer;
		WorldRenderer worldRenderer = renderer.getWorldRenderer();
		GUIRenderer guiRenderer = renderer.getGUIRenderer();
		renderer.loadPostRenderThread();
		while(inst.isRunning) {
			//Call Render Functionality//

			//Render World + entity(included)//
			try {
				if(OpenVoxel.getInstance().currentServer != null) {
					World world = OpenVoxel.getInstance().currentServer.getMyWorld();
					if (world != null) {//IGNORE IF WORLD DOESN'T EXIST
						worldRenderer.renderWorld(world);// TODO: 25/08/2016 Work Out Chunk Unload and Load
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
			}

			//Render GUI//
			try {
				guiRenderer.beginDraw();
				synchronized (GUI.class) {
					GUI.getStack().descendingIterator().forEachRemaining(guiRenderer::DisplayScreen);
				}
				guiRenderer.DisplayScreen(ScreenDebugInfo.instance);
			}catch (Exception e) {
				CrashReport crashReport = new CrashReport("Exception Drawing GUI").caughtException(e);
				OpenVoxel.reportCrash(crashReport);
			}

			//Finish//
			renderer.nextFrame();
			FPS_TIMER.notifyEvent();
		}
		renderer.kill();
	}
}
