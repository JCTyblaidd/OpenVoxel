package net.openvoxel.client.renderer;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.gui.ScreenDebugInfo;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.CrashReport;

import java.util.Iterator;

public class GuiDrawTask implements Runnable {

	private AsyncBarrier barrier;
	private BaseGuiRenderer guiRenderer;
	private int width;
	private int height;

	public void update(AsyncBarrier barrier,GraphicsAPI api) {
		this.barrier = barrier;
		guiRenderer = api.getGuiRenderer();
		width = ClientInput.currentWindowFrameSize.x;
		height = ClientInput.currentWindowFrameSize.y;
	}

	@Override
	public void run() {
		try {
			//Check if dirty is possible
			boolean guiDirty = ScreenDebugInfo.debugLevel != ScreenDebugInfo.GUIDebugLevel.NONE;
			guiDirty |= !guiRenderer.allowDrawCaching();

			//Check if dirty
			if(!guiDirty) {
				for (Screen screen : GUI.getStack()) {
					guiDirty |= screen.isDrawDirty();
				}
			}

			//Update if dirty
			if(guiDirty) {
				guiRenderer.StartDraw(width,height);
				Iterator<Screen> iterate = GUI.getStack().descendingIterator();
				while(iterate.hasNext()) {
					iterate.next().DrawScreen(guiRenderer);
				}

				//Debug Screen Renderer//
				ScreenDebugInfo.instance.DrawScreen(guiRenderer);
			}

			//Finish Draw
			guiRenderer.finishDraw(guiDirty);
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Error Drawing GUI");
			report.caughtException(ex);
			OpenVoxel.reportCrash(report);
		}finally {
			barrier.completeTask();
		}
	}

}
