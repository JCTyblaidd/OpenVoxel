package net.openvoxel.client.renderer.vk2;

import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.vk2.core.VulkanState;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.event.EventListener;

public class VulkanRenderer implements GlobalRenderer,EventListener {

	public final VulkanState state;

	public VulkanRenderer() {
		state = new VulkanState();
	}

	////////////////////////////////////
	/// Configuration State Changing ///
	////////////////////////////////////

	@Override
	public void setTargetFPS(int target) {

	}

	@Override
	public int getTargetFPS() {
		return 0;
	}

	@Override
	public void requestScreenshot() {

	}

	@Override
	public void setVSyncState(VSyncType state) {

	}

	@Override
	public boolean isVSyncSupported(VSyncType type) {
		return false;
	}

	@Override
	public VSyncType getVSyncState() {
		return null;
	}

	@Override
	public void setFullscreenState(ScreenType state) {

	}

	@Override
	public boolean isFullscreenSupported(ScreenType type) {
		return false;
	}

	@Override
	public ScreenType getFullscreenState() {
		return null;
	}

	@Override
	public void requestSettingsChange(RenderConfig newConfig) {

	}

	@Override
	public WorldRenderer getWorldRenderer() {
		return null;
	}

	@Override
	public DisplayHandle getDisplayHandle() {
		return null;
	}

	@Override
	public GUIRenderer getGUIRenderer() {
		return null;
	}

	@Override
	public void loadPreRenderThread() {

	}

	@Override
	public void loadPostRenderThread() {

	}

	@Override
	public String getShaderPostfix() {
		return null;
	}

	@Override
	public void nextFrame() {

	}

	@Override
	public void kill() {

	}

	@Override
	public IconAtlas getBlockAtlas() {
		return null;
	}

	@Override
	public Screen getGUIConfigElements() {
		return null;
	}
}
