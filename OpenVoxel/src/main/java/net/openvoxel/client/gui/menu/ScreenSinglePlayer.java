package net.openvoxel.client.gui.menu;

import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.gui_framework.GUIButton;
import net.openvoxel.client.gui_framework.GUIColour;
import net.openvoxel.client.gui_framework.Screen;

/**
 * Created by James on 11/09/2016.
 *
 * Allow For The Creation / Loading of SinglePlayer Games
 */
public class ScreenSinglePlayer extends Screen{

	public GUIColour colourHint;
	public GUIColour areaBackground;
	public GUIColour areaOptions;
	public GUIButton buttonMainMenu;
	public GUIButton buttonSettings;
	public GUIButton buttonLoad;
	public GUIButton buttonNew;

	private ScreenMainMenu mainMenu;

	public ScreenSinglePlayer(ScreenMainMenu mainMenu) {
		this();
		this.mainMenu = mainMenu;
	}

	private ScreenSinglePlayer() {
		colourHint = new GUIColour(0x66000000);
		areaBackground = new GUIColour(0x66000000);
		areaOptions = new GUIColour(0x66000000);
		buttonMainMenu = new GUIButton("Back");
		buttonSettings = new GUIButton("Settings");
		buttonLoad = new GUIButton("Load Game");
		buttonNew = new GUIButton("New Game");

		colourHint.setupFullscreen();
		areaBackground.setPosition(0.F,0.F,50,50);
		areaBackground.setSize(1.0F,1.0F,-100,-100);

		areaOptions.setPosition(0.F,1.0F,50,-125);
		areaOptions.setSize(1.0F,0.0F,-100,75);

		buttonMainMenu.setupOffsetTo(areaOptions,10,10,150,30);
		buttonSettings.setupOffsetTo(areaOptions,210,10,150,30);
		buttonLoad.setupOffsetTo(areaOptions,420,10,150,30);
		buttonNew.setupOffsetTo(areaOptions,630,10,150,30);

		guiObjects.add(colourHint);
		guiObjects.add(areaBackground);
		guiObjects.add(areaOptions);
		guiObjects.add(buttonMainMenu);
		guiObjects.add(buttonSettings);
		guiObjects.add(buttonLoad);
		guiObjects.add(buttonNew);

		buttonMainMenu.setAction(this::onMainMenu);
	}

	private void onMainMenu(GUIButton guiButton) {
		GUI.removeScreen(this);
		GUI.addScreen(mainMenu);
	}


}
