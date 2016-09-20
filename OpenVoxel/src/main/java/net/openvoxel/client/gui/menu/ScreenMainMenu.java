package net.openvoxel.client.gui.menu;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.gui.menu.settings.ScreenSettings;
import net.openvoxel.client.gui_framework.*;
import net.openvoxel.server.LocalServer;

/**
 * Created by James on 01/09/2016.
 *
 * Main Menu
 */
public class ScreenMainMenu extends Screen{

	public GUIObjectImage backgroundImage;
	public GUIButton buttonSinglePlayer;
	public GUIButton buttonMultiPlayer;
	public GUIButton buttonSettings;
	public GUIButton buttonQuit;
	public GUIText mainText;
	public GUIColour background;

	public ScreenMainMenu() {
		backgroundImage = new GUIObjectImage("gui/BG");
		backgroundImage.setupFullscreen();
		guiObjects.add(backgroundImage);

		buttonSinglePlayer = new GUIButton("Single Player");
		buttonMultiPlayer = new GUIButton("Multi Player");
		buttonSettings = new GUIButton("Settings");
		buttonQuit = new GUIButton("Quit");
		mainText = new GUIText("OpenVoxel");

		background = new GUIColour(0xFF000000,0x00000000,false);

		mainText.setupAbsSizeTargeted(0.25F,0.4F,0,140);
		buttonSinglePlayer.setupOffsetTo(mainText,-125,-30,150,30);
		buttonMultiPlayer.setupOffsetTo(mainText,-125,10,150,30);
		buttonSettings.setupOffsetTo(mainText,-125,50,150,30);
		buttonQuit.setupOffsetTo(mainText,-125,90,150,30);

		background.setupOffsetTo(mainText,-130,-100,260,500);//300);

		guiObjects.add(background);
		guiObjects.add(mainText);
		guiObjects.add(buttonSinglePlayer);
		guiObjects.add(buttonMultiPlayer);
		guiObjects.add(buttonSettings);
		guiObjects.add(buttonQuit);
/**
		GUILoadingCircle temp = new GUILoadingCircle();
		temp.setCentered(100,100);
		guiObjects.add(temp);
**/
		buttonSinglePlayer.setAction(this::onPressSinglePlayer);
		buttonMultiPlayer.setAction(this::onPressMultiPlayer);
		buttonSettings.setAction(this::onPressSettings);
		buttonQuit.setAction(this::onPressQuit);
	}

	public void onPressSinglePlayer(GUIButton button) {
		//TODO: convert to real code
		GUI.removeScreen(this);
		GUI.addScreen(new ScreenSinglePlayer());
		/**
			LocalServer myServer = new LocalServer();
			OpenVoxel.getInstance().HostServer(myServer);
			OpenVoxel.getInstance().clientConnectToLocalHost();
			GUI.removeAllScreens();
		 **/
	}
	public void onPressMultiPlayer(GUIButton button) {
		LocalServer myServer = new LocalServer();
		OpenVoxel.getInstance().HostServer(myServer);
		OpenVoxel.getInstance().clientConnectToLocalHost();
		GUI.removeAllScreens();
	}
	public void onPressSettings(GUIButton button) {
		//GUI.removeScreen(this);
		GUI.addScreen(new ScreenSettings());
	}
	public void onPressQuit(GUIButton button) {
		OpenVoxel.getInstance().AttemptShutdownSequence(false);
	}

}
