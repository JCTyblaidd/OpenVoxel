package net.openvoxel.client.gui.util;

import net.openvoxel.client.gui.widgets.display.*;
import net.openvoxel.client.gui.framework.Screen;

/**
 * Created by James on 14/09/2016.
 *
 * Screen Displayed while the program is loading
 */
public class ScreenLoading extends Screen {

	//private GUIObjectImage background;
	private GuiGearBG background;
	private GUIColour progressArea;
	private GUIProgressBar sectionProgress;
	private GUIProgressBar totalProgress;
	private GUIText currentTask;
	private GUIText currentMod;
	private int modID = -1;
	private int sectionID = -1;

	public ScreenLoading(int sectionCount,int modCount) {
		background = new GuiGearBG(0xFF252525);
		progressArea = new GUIColour(0xFF2e335e);
		sectionProgress = new GUIProgressBar(false);
		totalProgress = new GUIProgressBar(false);
		currentTask = new GUIText("--initial--",1.0F);
		currentMod = new GUIText("unknown",1.0F);
		sectionProgress.setMaxVal(modCount);
		totalProgress.setMaxVal(sectionCount);

		progressArea.setPosition(0.4F,0.4F,-100,-100);
		progressArea.setSize(0.2F,0.2F,200,200);

		totalProgress.setPosition(0.4F,0.6F,-50,0);
		totalProgress.setSize(0.2F,0,100,50);

		sectionProgress.setPosition(0.4F,0.4F,-50,25);
		sectionProgress.setSize(0.2F,0,100,50);

		currentTask.setPosition(0.4F,0.6F,-50,-75);
		currentTask.setSize(0.2F,0,100,50);

		currentMod.setPosition(0.4F,0.4F,-50,-50);
		currentMod.setSize(0.2F,0,100,50);

		guiObjects.add(background);
		guiObjects.add(progressArea);
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
