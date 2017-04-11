package net.openvoxel.client.gui.menu;

import net.openvoxel.client.gui_framework.GUIColour;
import net.openvoxel.client.gui_framework.Screen;

/**
 * Created by James on 11/09/2016.
 *
 * Allow For The Creation / Loading of SinglePlayer Games
 */
public class ScreenSinglePlayer extends Screen{

	GUIColour colourHint;

	public GUIColour bottomBar;
	public GUIColour loadGameBG;

	public ScreenSinglePlayer() {
		bottomBar = new GUIColour(0xFF00FF00);
		loadGameBG = new GUIColour(0x99000000);
		colourHint = new GUIColour(0x66000000);

		colourHint.setupFullscreen();
		loadGameBG.setCentered(100,100);

		guiObjects.add(colourHint);
		///////////////////////////
		guiObjects.add(loadGameBG);
		guiObjects.add(bottomBar);
	}


}
