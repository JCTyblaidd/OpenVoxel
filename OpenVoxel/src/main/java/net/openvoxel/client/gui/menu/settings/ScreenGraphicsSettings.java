package net.openvoxel.client.gui.menu.settings;

import net.openvoxel.client.gui.framework.GUI;
import net.openvoxel.client.gui.framework.Screen;
import net.openvoxel.client.gui.widgets.*;
import net.openvoxel.client.gui.widgets.group.GUIVScrollArea;
import net.openvoxel.client.gui.widgets.input.GUIButton;
import net.openvoxel.client.gui.widgets.input.GUISlider;
import net.openvoxel.client.gui.widgets.input.GUIToggleButton;
import net.openvoxel.client.gui.widgets.display.GUIColour;
import net.openvoxel.client.renderer.Renderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 20/09/2016.
 */
public class ScreenGraphicsSettings extends Screen {

	private Renderer renderer;

	private static final List<String> renderTypeStrings = List.of("Renderer: Voxel","Renderer: Environmental","Renderer: Deferred","Renderer: Forward");
	private static final List<String> particleStrings = List.of("Particles: ON","Particles: MINIMAL","Particles: NONE");
	private static final List<String> shadowMapStrings = List.of("Shadows: CASCADE","Shadows: SINGLE","Shadows: NONE");
	private static final List<String> tessellationStrings = List.of("Tessellation: PARALLAX","Tessellation: DISPLACEMENT","Tessellation: NONE");
	private static final List<String> fogStrings = List.of("Fog: ENABLED","Fog: DISABLED");
	private static final List<String> godRayStrings = List.of("God Rays: ENABLED","God Rays: DISABLED");
	private static final List<String> animateStrings = List.of("Animation: ENABLED","Animation: DISABLED");
	//private static final List<String> antiAliasStrings = List.of("Anti-alias: MXAA-4","Anti-alias: MXAA-2","Anti-alias: FXAA","Anti-alias: NONE");
	//private static final List<String> reflectionStrings = List.of("Reflection: HIGH","Reflection: MEDIUM","Reflection: LOW","Reflection: NONE");
	//private static final List<String> depthOfFieldStrings = List.of("Depth of Field: ENABLED","Depth of Field: DISABLED");
	//private static final List<String> transparencyStrings = List.of("Transparency: Depth-Peel","Transparency: Weighted Average","Transparency: Simple");

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

	public ScreenGraphicsSettings(Renderer renderer) {
		this.advSettings = null;
		this.renderer = renderer;

		//GUIColour background = new GUIColour(0xFF464646);
		//background.setupFullscreen();
		//guiObjects.add(background);

		section = new GUIVScrollArea();
		section.setPosition(0.35f,0.2f,-75,-100);
		section.setSize(0.3f,0.6f,150,200);
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

		/*
		GUIToggleButton antiAliasButton = new GUIToggleButton(antiAliasStrings,antiAliasStrings.get(0));
		setupConfig(antiAliasButton);

		GUIToggleButton reflectionButton = new GUIToggleButton(reflectionStrings,reflectionStrings.get(0));
		setupConfig(reflectionButton);

		GUIToggleButton depthOfFieldButton = new GUIToggleButton(depthOfFieldStrings,depthOfFieldStrings.get(0));
		setupConfig(depthOfFieldButton);

		GUIToggleButton transparencyButton = new GUIToggleButton(transparencyStrings,transparencyStrings.get(0));
		setupConfig(transparencyButton);
		*/

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
		return renderer.getTargetFrameRate();
	}

	private void setTargetFPS(int targetFPS) {
		if(targetFPS == 145) {
			renderer.setTargetFrameRate(Integer.MAX_VALUE);
		}else{
			renderer.setTargetFrameRate(targetFPS);
		}
	}

	private int getCurrentChunkRadius() {
		//TODO: IMPLEMENT PROPERLY
		return 16;
	}

	private void setChunkRadius(int chunkRadius) {
		//TODO: IMPLEMENT
	}

	private void setVSyncState(String state_str) {
		String id_state = state_str.substring("V-sync: ".length());
		renderer.setVSyncType(GraphicsAPI.VSyncType.valueOf(id_state));
	}

	private List<String> getVSyncSupport() {
		ArrayList<String> supported = new ArrayList<>();
		for(GraphicsAPI.VSyncType type : GraphicsAPI.VSyncType.values()) {
			if(renderer.isVSyncSupported(type)) {
				supported.add("V-sync: "+type.name());
			}
		}
		return supported;
	}

	private String getVSyncState() {
		return "V-sync: "+renderer.getVSync().name();
	}

	private void setScreenState(String state_str) {
		renderer.setScreenType(GraphicsAPI.ScreenType.valueOf(state_str));
	}

	private List<String> getScreenSupport() {
		ArrayList<String> supported = new ArrayList<>();
		for(GraphicsAPI.ScreenType type : GraphicsAPI.ScreenType.values()) {
			if(renderer.isScreenTypeSupported(type)) {
				supported.add(type.name());
			}
		}
		return supported;
	}

	private String getScreenState() {
		return renderer.getScreenType().name();
	}

	private void onAdvancedSettings() {
		if(advSettings != null) {
			GUI.addScreen(advSettings);
		}
	}
}
