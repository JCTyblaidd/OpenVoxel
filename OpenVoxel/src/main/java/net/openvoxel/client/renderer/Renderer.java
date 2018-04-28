package net.openvoxel.client.renderer;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.PerSecondTimer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.renderer.vk.VulkanRenderer;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.SubscribeEvents;
import net.openvoxel.common.event.input.KeyStateChangeEvent;
import net.openvoxel.common.event.input.WindowResizeEvent;
import net.openvoxel.common.event.window.WindowCloseRequestedEvent;
import net.openvoxel.files.FolderUtils;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.AsyncBarrier;
import net.openvoxel.utility.CrashReport;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F12;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

/**
 * Wrapper around the rendering api that is going to be used
 */
public final class Renderer implements EventListener {

	//Sub Objects
	private final Logger logger;
	private final GraphicsAPI api;
	private final PerSecondTimer frameRateTimer;
	private final GuiDrawTask guiDrawTask;

	//FrameRate Limiting
	private int targetFrameRate;
	private long previousFrameTimestamp;

	//State Changes
	private boolean screenshotRequest;
	private boolean changeStateRequest;
	private boolean reloadTextureRequest;
	private GraphicsAPI.VSyncType requestVSync;
	private GraphicsAPI.ScreenType requestScreen;

	public Renderer() {
		logger = Logger.getLogger("Renderer");
		frameRateTimer = new PerSecondTimer(64);
		guiDrawTask = new GuiDrawTask();
		targetFrameRate = Integer.MAX_VALUE;
		previousFrameTimestamp = 0L;

		screenshotRequest = false;
		changeStateRequest = false;
		reloadTextureRequest = false;
		requestVSync = null;
		requestScreen = null;

		OpenVoxel.registerEvents(this);

		//Initialize Graphics
		if(glfwInit()) {
			logger.Info("Initialized GLFW");
		}else {
			logger.Severe("Failed to initialize GLFW");
			CrashReport crashReport = new CrashReport("Failed to Initialize Renderer");
			crashReport.invalidState("glfwInit() == false");
			OpenVoxel.reportCrash(crashReport);
		}
		//Choose Renderer
		boolean flag_gl = OpenVoxel.getLaunchParameters().hasFlag("openGL");
		boolean flag_vk = OpenVoxel.getLaunchParameters().hasFlag("vulkan");
		if(flag_gl && flag_vk) {
			logger.Warning("Both OpenGL and Vulkan Requested - Choosing Vulkan");
			flag_gl = false;
		}
		//Initialize Renderer
		boolean vulkan_supported = glfwVulkanSupported();
		if(flag_vk && !vulkan_supported) {
			logger.Warning("Vulkan Requested But Not Found");
		}
		if(flag_gl && vulkan_supported) {
			logger.Warning("Vulkan Available But OpenGL Requested");
		}
		if(vulkan_supported && !flag_gl){
			logger.Info("Loading: Vulkan Renderer");
			api = new VulkanRenderer();
		}else {
			logger.Info("Loading: OGL3 Renderer");
			api = new VulkanRenderer();//TODO: IMPLEMENT OPENGL RENDERER!!!
			System.exit(0);//TODO: IMPLEMENT OpenGL Renderer
		}
	}

	public void close() {
		api.close();
		OpenVoxel.unregisterAllEvents(this);
	}


	///////////////////////////////
	/// Configuration Functions ///
	///////////////////////////////

	public float getFrameRate() {
		return frameRateTimer.getPerSecond();
	}

	public int getTargetFrameRate() {
		return targetFrameRate;
	}

	public void setTargetFrameRate(int target) {
		targetFrameRate = target;
	}

	public void requestScreenshot() {
		screenshotRequest = true;
	}

	public GraphicsAPI.VSyncType getVSync() {
		return api.getCurrentVSync();
	}

	public boolean isVSyncSupported(GraphicsAPI.VSyncType sync) {
		return api.isVSyncSupported(sync);
	}

	public void setVSyncType(GraphicsAPI.VSyncType sync) {
		if(api.isVSyncSupported(sync) && sync != api.getCurrentVSync()) {
			changeStateRequest = true;
			requestVSync = sync;
		}else{
			logger.Warning("Requested change to invalid VSync: " + sync);
		}
	}

	public GraphicsAPI.ScreenType getScreenType() {
		return api.getCurrentScreen();
	}

	public boolean isScreenTypeSupported(GraphicsAPI.ScreenType type) {
		return api.isScreenSupported(type);
	}

	public void setScreenType(GraphicsAPI.ScreenType type) {
		if(api.isScreenSupported(type) && type != api.getCurrentScreen()) {
			changeStateRequest = true;
			requestScreen = type;
		}else{
			logger.Warning("Requested change to invalid ScreenType: " + type);
		}
	}

	/////////////////////////
	/// Utility Functions ///
	/////////////////////////

	private void awaitFPSTarget() {
		if(targetFrameRate != Integer.MAX_VALUE) {
			long fps_target = targetFrameRate;
			long target_nanos = 1000000000L / fps_target;
			long currentTimestamp = System.nanoTime();
			long delta = currentTimestamp - previousFrameTimestamp;
			if (delta <= target_nanos) {
				int nano_wait = (int) (target_nanos - delta);
				try {
					Thread.sleep(nano_wait / 1000000, nano_wait % 1000000);
				} catch (Exception ignored) {
				}
			}
			previousFrameTimestamp = currentTimestamp;
		}
	}

	@SubscribeEvents
	public void onResizeEvent(WindowResizeEvent e) {
		changeStateRequest = true;
	}

	@SubscribeEvents
	public void onButtonPressed(KeyStateChangeEvent e) {
		if (e.GLFW_KEY == GLFW_KEY_F11 && e.GLFW_KEY_STATE == GLFW_PRESS) {
			boolean isFullscreen = getScreenType() != GraphicsAPI.ScreenType.WINDOWED;
			if(isFullscreen) {
				setScreenType(GraphicsAPI.ScreenType.WINDOWED);
			}else if(isScreenTypeSupported(GraphicsAPI.ScreenType.FULLSCREEN)) {
				setScreenType(GraphicsAPI.ScreenType.FULLSCREEN);
			}else if(isScreenTypeSupported(GraphicsAPI.ScreenType.BORDERLESS)){
				setScreenType(GraphicsAPI.ScreenType.BORDERLESS);
			}
		}else if (e.GLFW_KEY == GLFW_KEY_F12 && e.GLFW_KEY_STATE == GLFW_PRESS) {
			requestScreenshot();
		}
	}

	@SubscribeEvents
	public void onWindowCloseRequest(WindowCloseRequestedEvent e) {
		OpenVoxel.getInstance().AttemptShutdownSequence(false);
	}

	///////////////////////////
	/// Main Loop Functions ///
	///////////////////////////

	/**
	 * Prepare GPU for Streaming of data
	 *
	 * Present Image, vk
	 */
	public void prepareFrame() {
		awaitFPSTarget();
		//Handle Screenshots
		if(screenshotRequest) {
			screenshotRequest = false;
			GraphicsAPI.ScreenshotInfo screenshot = api.takeScreenshot();
			try {
				FolderUtils.saveScreenshot(screenshot);
			}catch(Exception ex) {
				ex.printStackTrace();
			} finally {
				screenshot.free();
			}
		}
		//Handle resize & vSync & window changes
		if(changeStateRequest) {
			changeStateRequest = false;
			api.startStateChange();
			if(requestVSync != null) {
				api.setVSync(requestVSync);
				requestVSync = null;
			}
			if(requestScreen != null) {
				api.setScreenType(requestScreen);
				requestScreen = null;
			}
			api.stopStateChange();
		}
		//Handle texture reloading
		if(reloadTextureRequest) {
			logger.Warning("Reloading of Textures is NYI!!!!");
		}
		api.acquireNextFrame();
		frameRateTimer.notifyEvent();
	}

	public void pollInputs() {
		glfwPollEvents();
	}

	/**
	 * Asynchronously generate updated chunk maps using the server data
	 */
	public void generateUpdatedChunks(ClientServer server, AsyncBarrier completeBarrier) {
		//TODO: IMPLEMENT Chunk Rendering
		completeBarrier.reset(1);
		completeBarrier.completeTask();
	}

	/**
	 * Invalidate all of the chunks
	 */
	public void invalidateAllChunks() {
		//TODO: IMPLEMENT Chunk Rendering
		logger.Warning("Invalidating all chunks is NYI!!!");
	}

	/**
	 * Stitch the atlas together
	 */
	public void stitchAtlas() {
		//TODO: IMPLEMENT Chunk Rendering
		logger.Warning("Stitching the World Atlas is NYI!!");
	}

	/**
	 * Asynchronously Draw the GUI in another thread
	 */
	public void startAsyncGUIDraw(AsyncBarrier completeBarrier) {
		completeBarrier.reset(1);
		guiDrawTask.update(completeBarrier,api);
		//TODO: ASYNC DISPATCH
		guiDrawTask.run();
	}

	/**
	 * Asynchronously generate command buffers
	 *  & then submit the frames to the GPU to present
	 */
	public void submitFrame() {
		//Start ASYNC Memory Updating//

		api.submitNextFrame();
	}

}
