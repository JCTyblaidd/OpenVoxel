package net.openvoxel.client.renderer.generic;

import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.textureatlas.IconAtlas;

/**
 * Created by James on 25/08/2016.
 *
 * Global Rendering Management Handle
 *
 * Contains Setting Configuration
 *
 * VSync ,Fullscreen and Frame-rate target are separate
 */
public interface GlobalRenderer {

	void setTargetFPS(int target);
	int getTargetFPS();

	enum VSyncType {
		V_SYNC_ENABLED("ENABLED"),
		V_SYNC_RELAXED("RELAXED"),
		V_SYNC_DISABLED("DISABLED"),
		V_SYNC_TRIPLE_BUFFERED("TRIPLE-BUFFERED");

		private String id;
		VSyncType(String id) {
			this.id = id;
		}
		public String getID() {
			return id;
		}
		public static VSyncType fromID(String id) {
			for(VSyncType value : values()) {
				if(value.id.equals(id)) return value;
			}
			return null;
		}
	}

	enum ScreenType {
		SCREEN_TYPE_WINDOWED("WINDOWED"),
		SCREEN_TYPE_BORDERLESS_WINDOW("BORDERLESS-WINDOW"),
		SCREEN_TYPE_FULLSCREEN("FULLSCREEN");

		private String id;
		ScreenType(String id) {
			this.id = id;
		}
		public String getID() {
			return id;
		}
		public static ScreenType fromID(String id) {
			for(ScreenType value :values()) {
				if(value.id.equals(id)) return value;
			}
			return null;
		}
	}

	void requestScreenshot();

	void setVSyncState(VSyncType state);
	boolean isVSyncSupported(VSyncType type);
	VSyncType getVSyncState();

	void setFullscreenState(ScreenType state);
	boolean isFullscreenSupported(ScreenType type);
	ScreenType getFullscreenState();

	void requestSettingsChange(RenderConfig newConfig);

	WorldRenderer getWorldRenderer();
	DisplayHandle getDisplayHandle();
	GUIRenderer   getGUIRenderer();

	void loadPreRenderThread();

	void loadPostRenderThread();

	String getShaderPostfix();

	void nextFrame();

	void kill();

	IconAtlas getBlockAtlas();

	Screen getGUIConfigElements();

}
