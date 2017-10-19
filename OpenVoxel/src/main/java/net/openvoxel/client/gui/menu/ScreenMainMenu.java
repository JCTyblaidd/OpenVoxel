package net.openvoxel.client.gui.menu;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.gui.menu.settings.ScreenSettings;
import net.openvoxel.client.gui_framework.*;

/**
 * Created by James on 01/09/2016.
 *
 * Main Menu
 */
public class ScreenMainMenu extends Screen{

	private GUIButton buttonSinglePlayer;
	private GUIButton buttonMultiPlayer;
	private GUIButton buttonSettings;
	private GUIButton buttonListMods;
	private GUIButton buttonReloadMods;
	private GUIButton buttonQuit;
	private GUIText mainText;
	private GUIColour background;

	public ScreenMainMenu() {

		buttonSinglePlayer = new GUIButton("Single Player");
		buttonMultiPlayer = new GUIButton("Multi Player");
		buttonSettings = new GUIButton("Settings");
		buttonListMods = new GUIButton("Mods");
		buttonReloadMods = new GUIButton("Reload");
		buttonQuit = new GUIButton("Quit");
		mainText = new GUIText("OpenVoxel");

		background = new GUIColour(0xFF000000,0x00000000,false);

		mainText.setupAbsSizeTargeted(0.25F,0.4F,0,70);//140
		buttonSinglePlayer.setupOffsetTo(mainText,-125,-30,150,30);
		buttonMultiPlayer.setupOffsetTo(mainText,-125,10,150,30);
		buttonSettings.setupOffsetTo(mainText,-125,50,150,30);
		buttonListMods.setupOffsetTo(mainText,-125,90,150,30);
		buttonReloadMods.setupOffsetTo(mainText,-125,130,150,30);
		buttonQuit.setupOffsetTo(mainText,-125,170,150,30);


		background.setupOffsetTo(mainText,-130,-100,260,500);//300);

		guiObjects.add(background);
		guiObjects.add(mainText);
		guiObjects.add(buttonSinglePlayer);
		guiObjects.add(buttonMultiPlayer);
		guiObjects.add(buttonSettings);
		guiObjects.add(buttonListMods);
		guiObjects.add(buttonReloadMods);
		guiObjects.add(buttonQuit);

		buttonSinglePlayer.setAction(this::onPressSinglePlayer);
		buttonMultiPlayer.setAction(this::onPressMultiPlayer);
		buttonSettings.setAction(this::onPressSettings);
		buttonReloadMods.setAction(this::onPressReload);
		buttonQuit.setAction(this::onPressQuit);
	}

	private void onPressSinglePlayer(GUIButton button) {
		GUI.removeScreen(this);
		GUI.addScreen(new ScreenSinglePlayer(this));
	}
	private void onPressMultiPlayer(GUIButton button) {
		GUI.addScreen(new ScreenMultiPlayer());
	}
	private void onPressSettings(GUIButton button) {
		GUI.addScreen(new ScreenSettings());
	}
	private void onPressQuit(GUIButton button) {
		OpenVoxel.getInstance().AttemptShutdownSequence(false);
	}

	private void onPressReload(GUIButton button) {
		OpenVoxel.reloadMods();
	}

}
