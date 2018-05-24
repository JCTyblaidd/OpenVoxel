package net.openvoxel.client.renderer;

import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.textureatlas.ArrayAtlas;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncTaskPool;
import org.jetbrains.annotations.TestOnly;

@TestOnly
public class TestGraphicsAPI implements GraphicsAPI {
	@Override
	public boolean acquireNextFrame() {
		return false;
	}

	@Override
	public boolean submitNextFrame(AsyncTaskPool pool, AsyncBarrier barrier, WorldDrawTask task) {
		return false;
	}

	@Override
	public void close() {

	}

	@Override
	public void startStateChange() {

	}

	@Override
	public void stopStateChange() {

	}

	@Override
	public void loadAtlas(ArrayAtlas blockAtlas) {

	}

	@Override
	public void freeAtlas() {

	}

	@Override
	public BaseGuiRenderer getGuiRenderer() {
		return null;
	}

	@Override
	public BaseWorldRenderer getWorldRenderer() {
		return null;
	}

	@Override
	public ScreenshotInfo takeScreenshot() {
		return null;
	}

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
}
