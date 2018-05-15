package net.openvoxel.client.gui.menu.settings;

import net.openvoxel.client.gui.framework.GUI;
import net.openvoxel.client.gui.framework.Screen;
import net.openvoxel.client.gui.widgets.input.GUIButton;
import net.openvoxel.client.gui.widgets.display.GUIColour;
import net.openvoxel.client.gui.widgets.display.GUIObjectImage;

/**
 * Created by James on 20/09/2016.
 *
 * Key Binding Manager Screen
 */
public class ScreenInputSettings extends Screen {

	private GUIObjectImage background;
	private GUIColour settingsArea;
	private GUIButton saveChanges;
	private GUIButton close;

	public ScreenInputSettings() {
		background = new GUIObjectImage("gui/BG");
		settingsArea = new GUIColour(0x999999FF);
		saveChanges = new GUIButton("Save Changes");
		close = new GUIButton("Close Without Saving");

		background.setupFullscreen();
		settingsArea.setSize(0,1,150,0);
		settingsArea.setPosition(0.5F,0,-75,0);
		saveChanges.setupOffsetTo(settingsArea,10,40,40,20);
		close.setupOffsetTo(settingsArea,40,40,40,20);

		guiObjects.add(background);
		guiObjects.add(settingsArea);
		guiObjects.add(saveChanges);
		guiObjects.add(close);

		saveChanges.setAction(this::saveChanges);
		close.setAction(this::close);
	}

	private void close(GUIButton guiButton) {
		GUI.removeScreen(this);
	}

	private void saveChanges(GUIButton guiButton) {
		GUI.removeScreen(this);
	}



}
