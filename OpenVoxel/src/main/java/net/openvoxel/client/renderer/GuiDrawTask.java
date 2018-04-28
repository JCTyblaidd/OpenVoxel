package net.openvoxel.client.renderer;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.utility.AsyncBarrier;
import net.openvoxel.utility.CrashReport;

public class GuiDrawTask implements Runnable {

	private AsyncBarrier barrier;
	private GraphicsAPI api;

	public void update(AsyncBarrier barrier,GraphicsAPI api) {
		this.barrier = barrier;
		this.api = api;
	}

	@Override
	public void run() {
		try {
			/*
			guiRenderer.Begin();
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
			*/
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Error Drawing GUI");
			report.caughtException(ex);
			OpenVoxel.reportCrash(report);
		}finally {
			barrier.completeTask();
		}
	}

}
