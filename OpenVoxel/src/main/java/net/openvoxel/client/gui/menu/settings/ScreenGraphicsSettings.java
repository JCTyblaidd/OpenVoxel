package net.openvoxel.client.gui.menu.settings;

import net.openvoxel.client.gui_framework.*;

/**
 * Created by James on 20/09/2016.
 */
public class ScreenGraphicsSettings extends Screen{


	public ScreenGraphicsSettings() {
		GUIColour background = new GUIColour(0xFF464646);
		background.setupFullscreen();
		guiObjects.add(background);

		GUIVScrollArea section = new GUIVScrollArea();
		section.setPosition(0.4f,0.2f,-50,-100);
		section.setSize(0.2f,0.6f,100,200);
		guiObjects.add(section);

		GUISlider targetFPS = new GUISlider(10,280,60,e -> "FPS: " + e);
		targetFPS.setSize(0,0,200,30);
		targetFPS.setPosition(0,0,10,10);
		section.add(targetFPS);

		GUIButton backButton = new GUIButton("Back");
		backButton.setAction(this::onBack);
		backButton.setCentered(40,40);
		section.add(backButton);
	}

	private void onBack() {
		GUI.removeLastScreen();
	}

}
