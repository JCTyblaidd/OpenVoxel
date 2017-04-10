package net.openvoxel.client.gui.menu.settings;

import net.openvoxel.client.gui.ScreenDebugInfo;
import net.openvoxel.client.gui_framework.*;

import java.util.Arrays;

/**
 * Created by James on 11/09/2016.
 *
 * Current Settings
 */
public class ScreenSettings extends Screen{

	public GUIButton settings_renderer;
	public GUIButton settings_input;
	public GUIButton settings_texture;
	public GUIButton settings_audio;
	public GUISlider setting_foV;
	public GUIToggleButton setting_debugmode;

	public GUIColour background;
	public GUIColour colourHint;

	public GUIButton backButton;

	public ScreenSettings() {
		background = new GUIColour(0xFF000000,0x00000000,false);
		setting_foV = new GUISlider(10,160,110,(fov) -> "FOV: " + fov);
		setting_foV.setUpdateFunc((slider,fov) -> {});
		settings_audio = new GUIButton("Audio Settings");
		settings_input = new GUIButton("Input Settings");
		settings_renderer = new GUIButton("Graphics Settings");
		settings_texture = new GUIButton("Resource Packs");
		setting_debugmode = new GUIToggleButton(Arrays.asList("No Debug","FPS Only","FPS+","Extreme Debug"),getDebugMode());
		colourHint = new GUIColour(0x66000000);
		backButton = new GUIButton("Go Back");

		colourHint.setupFullscreen();
		background.setCentered(600,700);
		setting_foV.setupOffsetTo(background,20,15,240,40);
		settings_renderer.setupOffsetTo(background,340,15,240,40);
		settings_input.setupOffsetTo(background,20,70,240,40);
		settings_texture.setupOffsetTo(background,340,70,240,40);
		settings_audio.setupOffsetTo(background,20,125,240,40);
		setting_debugmode.setupOffsetTo(background,340,125,240,40);
		backButton.setupOffsetTo(background,170,250,240,40);

		guiObjects.add(colourHint);
		guiObjects.add(background);
		guiObjects.add(setting_foV);
		guiObjects.add(settings_renderer);
		guiObjects.add(settings_input);
		guiObjects.add(settings_texture);
		guiObjects.add(settings_audio);
		guiObjects.add(backButton);
		guiObjects.add(setting_debugmode);

		backButton.setAction(this::onBack);
		setting_foV.setUpdateFunc(this::onFPSChange);
		setting_debugmode.setToggleAction(this::onDebugChange);
		settings_input.setAction(this::gotoInputSettings);
	}

	private void gotoInputSettings(GUIButton guiButton) {
		GUI.addScreen(new ScreenInputSettings());
	}

	private void onFPSChange(GUISlider slider,int value) {

	}

	private void onBack(GUIButton button) {
		GUI.removeScreen(this);
	}

	private String getDebugMode() {
		switch (ScreenDebugInfo.debugLevel.get()) {
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

	private void onDebugChange(GUIToggleButton button,String ID) {
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
		ScreenDebugInfo.debugLevel.set(level);
	}
}
