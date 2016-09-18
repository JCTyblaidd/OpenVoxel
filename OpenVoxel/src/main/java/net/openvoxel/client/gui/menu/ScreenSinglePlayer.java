package net.openvoxel.client.gui.menu;

import net.openvoxel.client.gui_framework.GUIButton;
import net.openvoxel.client.gui_framework.GUIColour;
import net.openvoxel.client.gui_framework.GUIObjectImage;
import net.openvoxel.client.gui_framework.Screen;

/**
 * Created by James on 11/09/2016.
 *
 * Allow For The Creation / Loading of SinglePlayer Games
 */
public class ScreenSinglePlayer extends Screen{

	public GUIObjectImage background;

	public GUIColour bottomBar;
	public GUIColour loadGameBG;

	public GUIButton playSelectedGame;
	public GUIButton createNewGame;

	public ScreenSinglePlayer() {
		bottomBar = new GUIColour(0xFF00FF00);
		loadGameBG = new GUIColour(0x99000000);
		background = new GUIObjectImage("gui/BG");

		background.setupFullscreen();
		bottomBar.setPosition(0,1,0,-150);
		bottomBar.setSize(1,0,0,150);
		loadGameBG.setPosition(0,0,0,100);
		loadGameBG.setSize(1,1,0,-100);

		guiObjects.add(background);
		///////////////////////////
		guiObjects.add(loadGameBG);
		guiObjects.add(bottomBar);
	}


}
