package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.textureatlas.IconAtlas;

/**
 * Created by James on 28/08/2016.
 *
 * Vulkan Renderer
 */
public class VkRenderer implements GlobalRenderer {

	public static Logger vkLog = Logger.getLogger("Vulkan");
	public static VkRenderer Vkrenderer;

	private VKDisplayHandle displayHandle;
	private VkDeviceState deviceState;

	private VKGUIRenderer guiRenderer;
	private VKWorldRenderer worldRenderer;

	private VKTexAtlas texAtlas = new VKTexAtlas();//TEMP//

	public VkRenderer() {
		vkLog.Info("Creating Vulkan Renderer");
		Vkrenderer = this;
	}

	@Override
	public void requestSettingsChange(RenderConfig newConfig) {

	}

	@Override
	public WorldRenderer getWorldRenderer() {
		return worldRenderer;
	}

	@Override
	public DisplayHandle getDisplayHandle() {
		return displayHandle;
	}

	@Override
	public GUIRenderer getGUIRenderer() {
		return guiRenderer;
	}

	@Override
	public void loadPreRenderThread() {
		deviceState = new VkDeviceState();
		displayHandle = new VKDisplayHandle(deviceState);
		guiRenderer = new VKGUIRenderer(deviceState);
		worldRenderer = new VKWorldRenderer();
	}

	@Override
	public void loadPostRenderThread() {
		//NO OP//
	}

	@Override
	public String getShaderPostfix() {
		return "spiv";
	}

	@Override
	public void nextFrame() {

	}

	@Override
	public void kill() {
		//worldRenderer.cleanup();
		guiRenderer.cleanup();
		deviceState.terminateAndFree();
	}

	@Override
	public IconAtlas getBlockAtlas() {
		return texAtlas;
	}
}
