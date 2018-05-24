package net.openvoxel.client.renderer.common;

import net.openvoxel.client.renderer.WorldDrawTask;
import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.textureatlas.ArrayAtlas;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncTaskPool;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface GraphicsAPI {

	////////////////////////////
	/// State Change Methods ///
	////////////////////////////

	boolean acquireNextFrame();
	boolean submitNextFrame(AsyncTaskPool pool, AsyncBarrier barrier, WorldDrawTask task);
	void close();

	void startStateChange();
	void stopStateChange();

	void loadAtlas(ArrayAtlas blockAtlas);
	void freeAtlas();

	/////////////////////////////
	/// Graphics Draw Methods ///
	/////////////////////////////

	BaseGuiRenderer getGuiRenderer();

	BaseWorldRenderer getWorldRenderer();

	///////////////////////
	/// Utility Methods ///
	///////////////////////

	/**
	 * Screenshot Information:
	 *  RGBA byte data,
	 *  width X height
	 */
	class ScreenshotInfo {
		public ByteBuffer bytes;
		public int width;
		public int height;
		public void free() {
			MemoryUtil.memFree(bytes);
		}
	}

	ScreenshotInfo takeScreenshot();

	/////////////////////////

	enum VSyncType {
		ENABLED,
		RELAXED,
		DISABLED,
		TRIPLE_BUFFERED
	}

	boolean isVSyncSupported(VSyncType type);
	VSyncType getCurrentVSync();
	void setVSync(VSyncType type);

	////////////////////////////

	enum ScreenType {
		WINDOWED,
		BORDERLESS,
		FULLSCREEN
	}

	boolean isScreenSupported(ScreenType type);
	ScreenType getCurrentScreen();
	void setScreenType(ScreenType type);


}
