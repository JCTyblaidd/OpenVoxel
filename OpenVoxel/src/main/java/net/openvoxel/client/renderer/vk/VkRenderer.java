package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.gui.menu.settings.ScreenGraphicsSettings;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.vulkan.KHRSurface.*;

/**
 * Created by James on 28/08/2016.
 *
 * Vulkan Renderer
 */
public class VkRenderer implements GlobalRenderer {

	private static Logger vkLog = Logger.getLogger("Vulkan").getSubLogger("Renderer");
	public static VkRenderer Vkrenderer;

	private VkDisplayHandle displayHandle;
	private VkDeviceState deviceState;

	private VkGUIRenderer guiRenderer;
	private VkWorldRenderer worldRenderer;

	private VkTexAtlas texAtlas;

	private AtomicBoolean needsRegen = new AtomicBoolean(false);
	private AtomicBoolean needsResize = new AtomicBoolean(false);
	private AtomicBoolean needsScreenshot = new AtomicBoolean(false);
	private AtomicReference<ScreenType> fullscreenType = new AtomicReference<>(ScreenType.SCREEN_TYPE_WINDOWED);
	private AtomicReference<VSyncType> vSyncType = new AtomicReference<>(VSyncType.V_SYNC_ENABLED);
	private AtomicInteger targetFPS = new AtomicInteger(Integer.MAX_VALUE);
	private long previousFramerateTimestamp = 0;

	@Override
	public void setTargetFPS(int target) {
		targetFPS.set(target);
	}

	@Override
	public int getTargetFPS() {
		return targetFPS.get();
	}

	@Override
	public void setVSyncState(VSyncType state) {
		if(isVSyncSupported(state) && vSyncType.get() != state) {
			vkLog.Info("Changing VSync State");
			vSyncType.set(state);
			needsResize.set(true);
		}
	}
	@Override
	public boolean isVSyncSupported(VSyncType type) {
		for(int i = 0; i < deviceState.presentModes.capacity(); i++) {
			final int mode = deviceState.presentModes.get(i);
			if(type == VSyncType.V_SYNC_ENABLED){
				if(mode == VK_PRESENT_MODE_FIFO_KHR) return true;
			}else if(type == VSyncType.V_SYNC_RELAXED) {
				if(mode == VK_PRESENT_MODE_FIFO_RELAXED_KHR) return true;
			}else if(type == VSyncType.V_SYNC_DISABLED) {
				if(mode == VK_PRESENT_MODE_IMMEDIATE_KHR) return true;
			}else if(type == VSyncType.V_SYNC_TRIPLE_BUFFERED) {
				if(mode == VK_PRESENT_MODE_MAILBOX_KHR) return true;
			}
		}
		return false;
	}
	@Override
	public VSyncType getVSyncState() {
		return vSyncType.get();
	}

	@Override
	public void setFullscreenState(ScreenType state) {
		if(isFullscreenSupported(state) && state != fullscreenType.get()) {
			vkLog.Info("Changing Screen State");
			fullscreenType.set(state);
			needsResize.set(true);
		}
	}
	@Override
	public boolean isFullscreenSupported(ScreenType type) {
		return type != ScreenType.SCREEN_TYPE_BORDERLESS_WINDOW;
	}
	@Override
	public ScreenType getFullscreenState() {
		return fullscreenType.get();
	}

	@Override
	public void requestScreenshot() {
		vkLog.Info("Screenshot Requested");
		needsScreenshot.set(true);
	}

	void toggleFullscreen() {
		vkLog.Info("Toggle Fullscreen Requested");
		ScreenType value = fullscreenType.get();
		if(value == ScreenType.SCREEN_TYPE_WINDOWED) {
			value = ScreenType.SCREEN_TYPE_FULLSCREEN;
		}else{
			value = ScreenType.SCREEN_TYPE_FULLSCREEN;
		}
		setFullscreenState(value);
	}

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
		vkLog.Info("Update Settings Requested");
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

	private void awaitFPSTarget() {
		int fps_target = targetFPS.get();
		if(fps_target != Integer.MAX_VALUE) {
			int target_nanos = 1000000000 / fps_target;
			long currentTimestamp = System.nanoTime();
			long delta = currentTimestamp - previousFramerateTimestamp;
			if (delta <= target_nanos) {
				int nano_wait = (int) (target_nanos - delta);
				try {
					Thread.sleep(nano_wait / 1000000, nano_wait % 1000000);
				} catch (Exception ignored) {
				}
			}
		}
	}

	private void startFPSTarget() {
		previousFramerateTimestamp = System.nanoTime();
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
		awaitFPSTarget();
		deviceState.submitNewWork(worldRenderer);
		deviceState.presentOnCompletion();
		if(needsScreenshot.get()) {
			needsScreenshot.set(false);
			deviceState.takeScreenshot();
		}
		if(needsRegen.get() || needsResize.get()) {
			needsResize.set(false);
			deviceState.setFullscreen(fullscreenType.get() != ScreenType.SCREEN_TYPE_WINDOWED);
			deviceState.setVSync(vSyncType.get());
			deviceState.recreateSwapChain(guiRenderer);
			if(needsRegen.get()) {
				texAtlas.cleanup();
				texAtlas.performStitchInternal();
				deviceState.reloadTexResources();
			}
			needsRegen.set(false);
			deviceState.acquireNextImage(true);
		}else {
			deviceState.acquireNextImage(false);
		}
		startFPSTarget();
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
	public VkTexAtlas getBlockAtlas() {
		return texAtlas;
	}

	@Override
	public Screen getGUIConfigElements() {
		return new ScreenGraphicsSettings(null);
	}

}
