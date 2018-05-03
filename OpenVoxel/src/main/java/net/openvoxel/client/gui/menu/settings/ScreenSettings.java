package net.openvoxel.client.gui.menu.settings;

import net.openvoxel.client.gui.ScreenDebugInfo;
import net.openvoxel.client.gui_framework.*;
import net.openvoxel.client.renderer.Renderer;

import java.util.Arrays;
import java.util.List;

/**
 * Created by James on 11/09/2016.
 *
 * Current Settings
 */
public class ScreenSettings extends Screen{

	private static final List<String> settings_list = Arrays.asList("No Debug","FPS Only","FPS+","Extreme Debug");
	private Renderer renderer;

	@Override
	public boolean hidesPreviousScreens() {
		return true;
	}

	public ScreenSettings(Renderer renderer) {
		this.renderer = renderer;

		GUIColour background = new GUIColour(0xFF000000, 0x00000000, false);
		GUISlider setting_foV = new GUISlider(10, 160, 110, (fov) -> "FOV: " + fov);
		GUIButton settings_audio = new GUIButton("Audio Settings");
		GUIButton settings_input = new GUIButton("Input Settings");
		GUIButton settings_renderer = new GUIButton("Graphics Settings");
		GUIButton settings_texture = new GUIButton("Resource Packs");
		GUIToggleButton setting_debug_mode = new GUIToggleButton(settings_list, getDebugMode());
		GUIColour colourHint = new GUIColour(0x66000000);
		GUIButton backButton = new GUIButton("Go Back");

		colourHint.setupFullscreen();
		background.setCentered(600,700);
		setting_foV.setupOffsetTo(background,20,15,240,40);
		settings_renderer.setupOffsetTo(background,340,15,240,40);
		settings_input.setupOffsetTo(background,20,70,240,40);
		settings_texture.setupOffsetTo(background,340,70,240,40);
		settings_audio.setupOffsetTo(background,20,125,240,40);
		setting_debug_mode.setupOffsetTo(background,340,125,240,40);
		backButton.setupOffsetTo(background,170,250,240,40);

		guiObjects.add(colourHint);
		guiObjects.add(background);
		guiObjects.add(setting_foV);
		guiObjects.add(settings_renderer);
		guiObjects.add(settings_input);
		guiObjects.add(settings_texture);
		guiObjects.add(settings_audio);
		guiObjects.add(backButton);
		guiObjects.add(setting_debug_mode);

		setting_foV.setUpdateFunc(this::onFOVChange);
		settings_audio.setAction(this::gotoAudioSettings);
		settings_input.setAction(this::gotoInputSettings);
		settings_renderer.setAction(this::gotoRendererSettings);
		settings_texture.setAction(this::gotoTextureSettings);
		setting_debug_mode.setToggleAction(this::onDebugChange);
		backButton.setAction(this::onBack);
	}

	private void gotoInputSettings() {
		GUI.addScreen(new ScreenInputSettings());
	}

	private void onFOVChange(GUISlider ignored, int value) {
		//OpenVoxel.getClientServer().getThePlayer().setFoV(value):
	}

	private void gotoAudioSettings() {

	}

	private void onBack() {
		GUI.removeScreen(this);
	}


	private void gotoRendererSettings() {
		GUI.addScreen(new ScreenGraphicsSettings(renderer));
	}

	private void gotoTextureSettings() {
		//TODO:
	}

	private String getDebugMode() {
		switch (ScreenDebugInfo.debugLevel) {
			case EXTREME_DETAIL:
				return "Extreme Debug";
			case  FPS:
				return "FPS Only";
			case FPS_BONUS:
				return "FPS+";
			case NONE:
				return "No Debug";
		}
		return "No Debug";
	}

	private void onDebugChange(String ID) {
		ScreenDebugInfo.GUIDebugLevel level;
		switch(ID) {
			case "No Debug":
				level = ScreenDebugInfo.GUIDebugLevel.NONE;
				break;
			case "FPS Only":
				level = ScreenDebugInfo.GUIDebugLevel.FPS;
				break;
			case "FPS+":
				level = ScreenDebugInfo.GUIDebugLevel.FPS_BONUS;
				break;
			case "Extreme Debug":
				level = ScreenDebugInfo.GUIDebugLevel.EXTREME_DETAIL;
				break;
			default:
				level = ScreenDebugInfo.GUIDebugLevel.NONE;
				break;
		}
		ScreenDebugInfo.debugLevel = level;
	}
}
