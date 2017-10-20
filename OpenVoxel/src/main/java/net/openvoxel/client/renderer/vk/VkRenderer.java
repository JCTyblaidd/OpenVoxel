package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.textureatlas.IconAtlas;

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

	private VkTexAtlas texAtlas;

	private AtomicBoolean needsRegen = new AtomicBoolean(false);
	private AtomicBoolean needsResize = new AtomicBoolean(false);

	public void markAsRegenRequired() {
		needsRegen.set(true);
	}

	public void markAsResizeRequired() {
		needsResize.set(true);
	}

	public VkRenderer() {
		vkLog.Info("Creating Vulkan Renderer");
		Vkrenderer = this;
	}

	@Override
	public void requestSettingsChange(RenderConfig newConfig) {
		//TODO:
	}

	@Override
	public VkWorldRenderer getWorldRenderer() {
		return worldRenderer;
	}

	@Override
	public VkDisplayHandle getDisplayHandle() {
		return displayHandle;
	}

	@Override
	public VkGUIRenderer getGUIRenderer() {
		return guiRenderer;
	}

	@Override
	public void loadPreRenderThread() {
		deviceState = new VkDeviceState();
		displayHandle = new VkDisplayHandle(this,deviceState);
		texAtlas = new VkTexAtlas();
		worldRenderer = new VkWorldRenderer(deviceState);
	}

	@Override
	public void loadPostRenderThread() {
		guiRenderer = new VkGUIRenderer(deviceState);
		deviceState.acquireNextImage(true);
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
		deviceState.submitNewWork(worldRenderer);
		deviceState.presentOnCompletion();
		if(needsRegen.get() || needsResize.get()) {
			needsResize.set(false);
			deviceState.recreateSwapChain(guiRenderer);
			if(needsRegen.get()) {
				System.out.println("##Resource Regen Not Implemented##");
				//TODO: regen resource images & etc
			}
			needsRegen.set(false);
			deviceState.acquireNextImage(true);
		}else {
			deviceState.acquireNextImage(false);
		}
	}

	@Override
	public void kill() {
		texAtlas.cleanup();
		worldRenderer.cleanup();
		displayHandle.cleanup();
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
