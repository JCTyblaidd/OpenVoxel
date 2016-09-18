package net.openvoxel.client.renderer.generic;

import net.openvoxel.client.renderer.generic.config.RenderConfig;

/**
 * Created by James on 25/08/2016.
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
}
