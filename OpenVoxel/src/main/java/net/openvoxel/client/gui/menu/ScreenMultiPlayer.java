package net.openvoxel.client.gui.menu;

import net.openvoxel.client.gui_framework.*;
import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 14/09/2016.
 *
 * Load Scan For Multi-player Data
 */
public class ScreenMultiPlayer extends Screen{

	public GUIObjectImage background;

	public GUIButton connect;
	public GUIButton addServer;
	public GUIButton directConnect;

	public ScreenMultiPlayer() {

	}

	public static class GUIMultiPlayerServer extends GUIObjectSizable {
		@Override
		public void Draw(GUIRenderer.GUITessellator drawHandle) {

		}
	}

}
