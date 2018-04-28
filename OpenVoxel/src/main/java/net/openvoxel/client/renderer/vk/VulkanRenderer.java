package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.common.event.EventListener;

public class VulkanRenderer implements EventListener, GraphicsAPI {

	public final VulkanState state;

	public VulkanRenderer() {
		state = new VulkanState();
	}

	@Override
	public void close() {
		state.close();
	}


	@Override
	public void acquireNextFrame() {

	}

	@Override
	public void submitNextFrame() {

	}

	@Override
	public void startStateChange() {

	}

	@Override
	public void stopStateChange() {

	}

	/////////////////////
	/// State Changes ///
	/////////////////////

	@Override
	public ScreenshotInfo takeScreenshot() {
		return null;
	}

	///////////////////////////////////

	@Override
	public boolean isVSyncSupported(VSyncType type) {
		return false;
	}

	@Override
	public VSyncType getCurrentVSync() {
		return null;
	}

	@Override
	public void setVSync(VSyncType type) {

	}

	////////////////////////////////////

	@Override
	public boolean isScreenSupported(ScreenType type) {
		return false;
	}

	@Override
	public ScreenType getCurrentScreen() {
		return null;
	}

	@Override
	public void setScreenType(ScreenType type) {

	}

	////////////////////////////////////
	/// Configuration State Changing ///
	////////////////////////////////////

	/*

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
	public String getShaderPostfix() {
		return null;
	}


	@Override
	public IconAtlas getBlockAtlas() {
		return null;
	}

	@Override
	public Screen getGUIConfigElements() {
		return null;
	}

	**/
}
