package net.openvoxel.client.renderer.common;

import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface GraphicsAPI {

	////////////////////////////
	/// State Change Methods ///
	////////////////////////////

	void acquireNextFrame();
	void submitNextFrame();
	void close();

	void startStateChange();
	void stopStateChange();

	/////////////////////////////
	/// Graphics Draw Methods ///
	/////////////////////////////

	BaseGuiRenderer getGuiRenderer();


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
