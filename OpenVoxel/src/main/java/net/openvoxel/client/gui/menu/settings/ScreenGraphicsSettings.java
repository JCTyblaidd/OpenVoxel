package net.openvoxel.client.gui.menu.settings;

import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.gui_framework.*;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 20/09/2016.
 */
public class ScreenGraphicsSettings extends Screen {

	private static final List<String> screenModeStrings = List.of("Fullscreen","Borderless Window","Windowed");
	private static final List<String> particleStrings = List.of("Particles: ON","Particles: MINIMAL","Particles: NONE");
	private static final List<String> shadowMapStrings = List.of("Shadows: CASCADE","Shadows: SINGLE","Shadows: NONE");
	private static final List<String> tessellationStrings = List.of("Tessellation: PARALLAX","Tessellation: DISPLACEMENT","Tessellation: NONE");
	private static final List<String> fogStrings = List.of("Fog: ENABLED","Fog: DISABLED");
	private static final List<String> godRayStrings = List.of("God Rays: ENABLED","God Rays: DISABLED");
	private static final List<String> animateStrings = List.of("Animation: ENABLED","Animation: DISABLED");
	private static final List<String> antiAliasStrings = List.of("Anti-alias: MXAA-4","Anti-alias: MXAA-2","Anti-alias: FXAA","Anti-alias: NONE");
	private static final List<String> reflectionStrings = List.of("Reflection: HIGH","Reflection: MEDIUM","Reflection: LOW","Reflection: NONE");
	private static final List<String> depthOfFieldStrings = List.of("Depth of Field: ENABLED","Depth of Field: DISABLED");
	private static final List<String> transparencyStrings = List.of("Transparency: Depth-Peel","Transparency: Weighted Average","Transparency: Simple");

	private int currentLeft = 10;
	private int currentRight = 10;
	private GUIVScrollArea section;
	private Screen advSettings;


	private void setupConfig(GUIObjectSizable sizable) {
		sizable.setSize(0,0,240,40);
		if(currentLeft <= currentRight) {
			sizable.setPosition(0,0,10,currentLeft);
			currentLeft += 50;
		}else{
			sizable.setPosition(1,0,-250,currentRight);
			currentRight += 50;
		}
		section.add(sizable);
	}

	public ScreenGraphicsSettings(Screen advSettings) {
		this.advSettings = advSettings;

		GUIColour background = new GUIColour(0xFF464646);
		background.setupFullscreen();
		guiObjects.add(background);

		section = new GUIVScrollArea();
		section.setPosition(0.4f,0.2f,-50,-100);
		section.setSize(0.5f,0.6f,-100,200);
		guiObjects.add(section);

		GUISlider targetFPS = new GUISlider(10,145,getCurrentTargetFPS(),(e) -> {
			if(e == 145) return "FPS: Unlimited";
			return "FPS: " + e;
		});
		targetFPS.setUpdateFunc(this::setTargetFPS);
		setupConfig(targetFPS);

		GUISlider chunkRadius = new GUISlider(4,64,getCurrentChunkRadius(),e -> "Chunk Radius: "+e);
		chunkRadius.setUpdateFunc(this::setChunkRadius);
		setupConfig(chunkRadius);

		GUIToggleButton vSyncButton = new GUIToggleButton(getVSyncSupport(),getVSyncState());
		vSyncButton.setToggleAction(this::setVSyncState);
		setupConfig(vSyncButton);

		GUIToggleButton screenButton = new GUIToggleButton(getScreenSupport(),getScreenState());
		screenButton.setToggleAction(this::setScreenState);
		setupConfig(screenButton);

		GUIToggleButton particleButton = new GUIToggleButton(particleStrings,particleStrings.get(0));
		setupConfig(particleButton);

		GUIToggleButton shadowButton = new GUIToggleButton(shadowMapStrings,shadowMapStrings.get(0));
		setupConfig(shadowButton);

		GUIToggleButton tessellateButton = new GUIToggleButton(tessellationStrings,tessellationStrings.get(0));
		setupConfig(tessellateButton);

		GUIToggleButton fogButton = new GUIToggleButton(fogStrings,fogStrings.get(0));
		setupConfig(fogButton);

		GUIToggleButton godRayButton = new GUIToggleButton(godRayStrings,godRayStrings.get(0));
		setupConfig(godRayButton);

		GUIToggleButton animateButton = new GUIToggleButton(animateStrings,animateStrings.get(0));
		setupConfig(animateButton);

		GUIToggleButton antiAliasButton = new GUIToggleButton(antiAliasStrings,antiAliasStrings.get(0));
		setupConfig(antiAliasButton);

		GUIToggleButton reflectionButton = new GUIToggleButton(reflectionStrings,reflectionStrings.get(0));
		setupConfig(reflectionButton);

		GUIToggleButton depthOfFieldButton = new GUIToggleButton(depthOfFieldStrings,depthOfFieldStrings.get(0));
		setupConfig(depthOfFieldButton);

		GUIToggleButton transparencyButton = new GUIToggleButton(transparencyStrings,transparencyStrings.get(0));
		setupConfig(transparencyButton);

		GUIButton advancedSettings = new GUIButton("Advanced Settings");
		advancedSettings.setAction(this::onAdvancedSettings);
		setupConfig(advancedSettings);

		GUIButton backButton = new GUIButton("Back");
		backButton.setPosition(0.5f,1,-140,-50);
		backButton.setSize(0,0,300,30);
		backButton.setAction(this::onBack);
		guiObjects.add(backButton);
	}

	private void onBack() {
		GUI.removeLastScreen();
	}

	private int getCurrentTargetFPS() {
		return Renderer.renderer.getTargetFPS();
	}

	private void setTargetFPS(int targetFPS) {
		if(targetFPS == 145) {
			Renderer.renderer.setTargetFPS(Integer.MAX_VALUE);
		}else{
			Renderer.renderer.setTargetFPS(targetFPS);
		}
	}

	private int getCurrentChunkRadius() {
		//TODO: IMPLEMENT PROPERLY
		return 16;
	}

	private void setChunkRadius(int chunkRadius) {
		//NO OP//
	}

	private void setVSyncState(String state_str) {
		String id_state = state_str.substring("V-sync: ".length());
		Renderer.renderer.setVSyncState(GlobalRenderer.VSyncType.fromID(id_state));
	}

	private List<String> getVSyncSupport() {
		ArrayList<String> supported = new ArrayList<>();
		for(GlobalRenderer.VSyncType type : GlobalRenderer.VSyncType.values()) {
			if(Renderer.renderer.isVSyncSupported(type)) {
				supported.add("V-sync: " + type.getID());
			}
		}
		return supported;
	}

	private String getVSyncState() {
		return "V-sync: " + Renderer.renderer.getVSyncState().getID();
	}

	private void setScreenState(String state_str) {
		Renderer.renderer.setFullscreenState(GlobalRenderer.ScreenType.fromID(state_str));
	}

	private List<String> getScreenSupport() {
		ArrayList<String> supported = new ArrayList<>();
		for(GlobalRenderer.ScreenType type : GlobalRenderer.ScreenType.values()) {
			if(Renderer.renderer.isFullscreenSupported(type)) {
				supported.add(type.getID());
			}
		}
		return supported;
	}

	private String getScreenState() {
		return Renderer.renderer.getFullscreenState().getID();
	}

	private void onAdvancedSettings() {
		if(advSettings != null) {
			GUI.addScreen(advSettings);
		}
	}
}
