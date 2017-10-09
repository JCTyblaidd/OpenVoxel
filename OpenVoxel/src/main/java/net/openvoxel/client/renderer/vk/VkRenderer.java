package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.textureatlas.IconAtlas;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 28/08/2016.
 *
 * Vulkan Renderer
 */
public class VkRenderer implements GlobalRenderer {

	public static Logger vkLog = Logger.getLogger("Vulkan");
	public static VkRenderer Vkrenderer;

	private VkDisplayHandle displayHandle;
	private VkDeviceState deviceState;

	private VkGUIRenderer guiRenderer;
	private VkWorldRenderer worldRenderer;

	private VkTexAtlas texAtlas = new VkTexAtlas();//TEMP//

	private AtomicBoolean needsRegen = new AtomicBoolean();

	public void markAsRegenRequired() {
		needsRegen.set(true);
	}

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
		displayHandle = new VkDisplayHandle(deviceState);
	}

	@Override
	public void loadPostRenderThread() {
		guiRenderer = new VkGUIRenderer(deviceState);
		worldRenderer = new VkWorldRenderer();
	}

	@Override
	public String getShaderPostfix() {
		return "vksl";
	}



	/**
	 * Handle split between frames
	 *
	 * Swaps between work generation and then this function
	 *
	 * [Work Gen] -> Submit -> Present -> Acquire
	 */
	@Override
	public void nextFrame() {
		if(needsRegen.get()) {
			needsRegen.set(false);
			deviceState.recreateSwapChain();
			//TODO: regen resource images & etc
		}
		deviceState.submitNewWork();
		deviceState.presentOnCompletion();
		deviceState.acquireNextImage();
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

	@Override
	public Screen getGUIConfigElements() {
		return null;
	}
}
