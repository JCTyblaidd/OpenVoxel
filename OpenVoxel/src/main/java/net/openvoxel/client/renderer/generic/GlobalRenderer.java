package net.openvoxel.client.renderer.generic;

import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.textureatlas.IconAtlas;

/**
 * Created by James on 25/08/2016.
 *
 * Global Rendering Management Handle
 */
public interface GlobalRenderer {

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
