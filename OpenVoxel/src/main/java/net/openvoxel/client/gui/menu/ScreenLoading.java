package net.openvoxel.client.gui.menu;

import net.openvoxel.client.gui.widgets.GUIObjectImage;
import net.openvoxel.client.gui.widgets.GUIProgressBar;
import net.openvoxel.client.gui.widgets.GUIText;
import net.openvoxel.client.gui.framework.Screen;
import net.openvoxel.client.renderer.Renderer;

/**
 * Created by James on 14/09/2016.
 *
 * Screen Displayed while the program is loading
 */
public class ScreenLoading extends Screen {

	private GUIObjectImage background;
	private GUIProgressBar sectionProgress;
	private GUIProgressBar totalProgress;
	private GUIText currentTask;
	private GUIText currentMod;
	private int modID = -1;
	private int sectionID = -1;

	public ScreenLoading(int sectionCount,int modCount) {
		background = new GUIObjectImage("gui/BG");
		background.setupFullscreen();
		sectionProgress = new GUIProgressBar(false);
		totalProgress = new GUIProgressBar(false);
		currentTask = new GUIText("--initial--");
		currentMod = new GUIText("unknown");
		sectionProgress.setMaxVal(modCount);
		totalProgress.setMaxVal(sectionCount);

		totalProgress.setCentered(200,40);
		sectionProgress.setupOffsetTo(totalProgress,0,-50,200,40);
		currentTask.setupOffsetTo(totalProgress,280,0,80,40);
		currentMod.setupOffsetTo(sectionProgress,280,0,80,40);

		guiObjects.add(background);
		guiObjects.add(sectionProgress);
		guiObjects.add(totalProgress);
		guiObjects.add(currentTask);
		guiObjects.add(currentMod);
	}

	public void startMod(String name) {
		modID++;
		sectionProgress.setCurrent(modID);
		currentMod.updateText(name);
	}

	public void startSection(String name) {
		modID = -1;
		sectionID++;
		totalProgress.setCurrent(sectionID);
		currentTask.updateText(name);
	}
}
