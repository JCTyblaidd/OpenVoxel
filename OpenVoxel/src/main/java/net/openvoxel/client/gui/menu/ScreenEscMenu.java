package net.openvoxel.client.gui.menu;

import net.openvoxel.client.gui.widgets.input.GUIButton;
import net.openvoxel.client.gui.widgets.display.GUIText;
import net.openvoxel.client.gui.framework.Screen;

/**
 * Created by James on 11/09/2016.
 *
 * Screen Opened Via Pressing Escape
 */
public class ScreenEscMenu extends Screen{

	public GUIText title;
	public GUIButton backToGame;
	public GUIButton options;
	public GUIButton achievements;
	public GUIButton quitGame;

	public ScreenEscMenu(boolean serverSide) {
		title = new GUIText("Game Menu");
		backToGame = new GUIButton("Return To Game");
		options = new GUIButton("Options");
		achievements = new GUIButton("Achievements");
		quitGame = new GUIButton(serverSide ? "Disconnect From Server" : "Save And Quit Game");




		guiObjects.add(title);
		guiObjects.add(backToGame);
		guiObjects.add(options);
		guiObjects.add(achievements);
		guiObjects.add(quitGame);
	}

}