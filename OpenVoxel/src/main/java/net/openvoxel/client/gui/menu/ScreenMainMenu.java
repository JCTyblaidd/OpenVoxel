package net.openvoxel.client.gui.menu;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.gui.framework.GUI;
import net.openvoxel.client.gui.framework.Screen;
import net.openvoxel.client.gui.menu.settings.ScreenSettings;
import net.openvoxel.client.gui.widgets.display.GUIGears;
import net.openvoxel.client.gui.widgets.input.GUIButton;
import net.openvoxel.client.gui.widgets.display.GUIColour;
import net.openvoxel.client.gui.widgets.display.GUIText;
import net.openvoxel.client.renderer.Renderer;

/**
 * Created by James on 01/09/2016.
 *
 * Main Menu
 */
public class ScreenMainMenu extends Screen {

	private Renderer renderer;

	public ScreenMainMenu(Renderer renderer) {
		this.renderer = renderer;

		GUIButton buttonSinglePlayer = new GUIButton("Single Player");
		GUIButton buttonMultiPlayer = new GUIButton("Multi Player");
		GUIButton buttonSettings = new GUIButton("Settings");
		GUIButton buttonListMods = new GUIButton("Mods");
		GUIButton buttonReloadMods = new GUIButton("Reload");
		GUIButton buttonQuit = new GUIButton("Quit");
		GUIText mainText = new GUIText("OpenVoxel");

		GUIColour background = new GUIColour(0xFF000000, 0x00000000, false);

		mainText.setupAbsSizeTargeted(0.25F,0.4F,150,70);//140
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

	private void onPressSinglePlayer() {
		GUI.removeScreen(this);
		GUI.addScreen(new ScreenSinglePlayer(this));
	}
	private void onPressMultiPlayer() {
		GUI.addScreen(new ScreenMultiPlayer());
	}
	private void onPressSettings() {
		GUI.addScreen(new ScreenSettings(renderer));
	}
	private void onPressQuit() {
		OpenVoxel.getInstance().AttemptShutdownSequence(false);
	}

	private void onPressReload() {
		OpenVoxel.reloadMods();
	}

}
