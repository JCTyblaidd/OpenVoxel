package net.openvoxel.client.renderer.vk;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.textureatlas.IconAtlas;

import static org.lwjgl.glfw.GLFW.*;

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
		guiRenderer = new VKGUIRenderer();
		worldRenderer = new VKWorldRenderer();
	}

	@Override
	public void loadPostRenderThread() {

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
		deviceState.terminateAndFree();
	}

	@Override
	public IconAtlas getBlockAtlas() {
		return null;
	}
}
