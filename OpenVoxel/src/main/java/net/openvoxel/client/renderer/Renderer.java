package net.openvoxel.client.renderer;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.PerSecondTimer;
import net.openvoxel.client.gui.util.ScreenDebugInfo;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.renderer.vk.VulkanRenderer;
import net.openvoxel.client.textureatlas.ArrayAtlas;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.SubscribeEvents;
import net.openvoxel.common.event.input.KeyStateChangeEvent;
import net.openvoxel.common.event.input.WindowRefreshEvent;
import net.openvoxel.common.event.window.WindowCloseRequestedEvent;
import net.openvoxel.files.util.FolderUtils;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.MathUtilities;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncTaskPool;
import net.openvoxel.utility.debug.UsageAnalyses;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
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
	private final WorldDrawTask worldDrawTask;
	private final AsyncTaskPool renderTaskPool;

	//Texture Atlas
	private ArrayAtlas blockAtlas;

	//FrameRate Limiting
	private int targetFrameRate;
	private long previousFrameTimestamp;

	//Display Configuration
	private float fieldOfView = 100.F;
	private int drawDistance = 16;

	//State Changes
	private boolean screenshotRequest;
	private boolean changeStateRequest;
	private boolean reloadResourceRequest;
	private GraphicsAPI.VSyncType requestVSync;
	private GraphicsAPI.ScreenType requestScreen;

	public Renderer() {
		logger = Logger.getLogger("Renderer");
		frameRateTimer = new PerSecondTimer(64);
		renderTaskPool = AsyncTaskPool.createTaskPool(
				"Render Pool",
				AsyncTaskPool.getClampedParameter(
						"renderWorkerCount",
						Runtime.getRuntime().availableProcessors(),
						0
				),
				AsyncTaskPool.getClampedParameter(
						"renderWorkerBufferSize",
						4096,
						1024
				)
		);
		renderTaskPool.start();

		blockAtlas = new ArrayAtlas();

		//Defaults
		targetFrameRate = 60;
		previousFrameTimestamp = 0L;

		screenshotRequest = false;
		changeStateRequest = false;
		reloadResourceRequest = false;
		requestVSync = null;
		requestScreen = null;

		OpenVoxel.registerEvents(this);

		//Initialize Graphics
		if(glfwInit()) {
			logger.Info("Initialized GLFW");
			GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			if(vidMode != null) targetFrameRate = vidMode.refreshRate();
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
			api = new VulkanRenderer(renderTaskPool.getWorkerCount());
		}else {
			logger.Info("Loading: OGL3 Renderer");
			api = null;//TODO: IMPLEMENT OpenGL Renderer...
			System.exit(0);//TODO: IMPLEMENT OpenGL Renderer
		}

		guiDrawTask = new GuiDrawTask(api);
		worldDrawTask = new WorldDrawTask(api,renderTaskPool.getWorkerCount());
	}

	public void close() {
		api.freeAtlas();
		blockAtlas.freeAllTextures();
		renderTaskPool.stop();
		api.close();
		OpenVoxel.unregisterAllEvents(this);
	}


	///////////////////////////////
	/// Configuration Functions ///
	///////////////////////////////

	public int getTargetFrameRate() {
		return targetFrameRate;
	}

	public void setTargetFrameRate(int target) {
		targetFrameRate = target;
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

	public float getFieldOfView() {
		return fieldOfView;
	}

	public void setFieldOfView(float FoV) {
		fieldOfView = MathUtilities.clamp(FoV,10.F,160.F);
	}

	//TODO: IMPLEMENT DRAW DISTANCE

	public IconAtlas getBlockAtlas() {
		return blockAtlas;
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
			previousFrameTimestamp = System.nanoTime();
		}
	}

	/*
	@SubscribeEvents
	public void onResizeEvent(WindowResizeEvent e) {
		//changeStateRequest = true;
	}
	*/

	@SubscribeEvents
	public void onRefreshEvent(WindowRefreshEvent e) {
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
			screenshotRequest = true;
		}else if(e.GLFW_KEY == GLFW_KEY_DELETE) {
			//TODO: REMOVE THIS FUNCTION WHEN IT IS NO LONGER NEEDED FOR DEBUGGING
			System.exit(0);
		}
	}

	@SubscribeEvents
	public void onWindowCloseRequest(WindowCloseRequestedEvent e) {
		OpenVoxel.getInstance().AttemptShutdownSequence(false);
	}

	///////////////////////////
	/// Main Loop Functions ///
	///////////////////////////

	private void takeScreenshot() {
		screenshotRequest = false;
		GraphicsAPI.ScreenshotInfo screenshot = api.takeScreenshot();
		try {
			if(screenshot != null) {
				FolderUtils.saveScreenshot(screenshot);
			}else{
				logger.Warning("Failed to take screenshot");
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			if(screenshot != null) {
				screenshot.free();
			}
		}
	}

	private void handleStateChange() {
		changeStateRequest = false;
		api.startStateChange();

		//Change Display Configuration
		if(requestVSync != null) {
			api.setVSync(requestVSync);
			requestVSync = null;
		}
		if(requestScreen != null) {
			api.setScreenType(requestScreen);
			requestScreen = null;
		}

		//Resource Shader Sources & Textures
		if(reloadResourceRequest) {
			//TODO: Invalidate all stored textures & update all of them
			logger.Warning("Reloading of Resources: Not Yet Implemented");
			reloadResourceRequest = false;
		}

		//TODO: changeGraphicsType (call invalidateAllChunks!!)

		api.stopStateChange();
	}


	/**
	 * Prepare GPU for Streaming of data
	 *
	 * Present Image, vk
	 */
	public void prepareFrame() {
		{
			UsageAnalyses.StartCPUSample("Await FPS", 0);
			awaitFPSTarget();
			UsageAnalyses.StopCPUSample();
		}
		//Handle Screenshots
		if(screenshotRequest) {
			UsageAnalyses.StartCPUSample("Screenshot",0);
			takeScreenshot();
			UsageAnalyses.StopCPUSample();
		}
		//Handle resize & vSync & window changes
		if(changeStateRequest) {
			logger.Info("Standard State Change");
			handleStateChange();
		}
		UsageAnalyses.StartCPUSample("API Acquire",0);
		boolean success = api.acquireNextFrame();
		UsageAnalyses.StopCPUSample();
		while (!success) {
			handleStateChange();
			logger.Info("Adv State Change");
			success = api.acquireNextFrame();
		}
		frameRateTimer.notifyEvent();
	}

	public void pollInputs() {
		glfwPollEvents();
	}

	/**
	 * Asynchronously generate updated chunk maps using the server data
	 */
	public void generateUpdatedChunks(ClientServer server, @NotNull AsyncBarrier completeBarrier) {
		if(server != null) {
			completeBarrier.reset(1);
			worldDrawTask.update(renderTaskPool,server,completeBarrier, fieldOfView,drawDistance);
			worldDrawTask.run();
		}else{
			worldDrawTask.ignore();
			completeBarrier.reset(0);
		}
	}

	/**
	 * Invalidate all of the chunks
	 */
	public void invalidateAllChunks() {
		worldDrawTask.freeAllData();
	}

	/**
	 * Stitch the atlas together
	 */
	public void stitchAtlas() {
		blockAtlas.performStitch();
		api.loadAtlas(blockAtlas);
		blockAtlas.freeAllTextures();
	}


	/**
	 * Asynchronously Draw the GUI in another thread
	 */
	public void startAsyncGUIDraw(@NotNull AsyncBarrier completeBarrier) {
		//Prepare the Async Task
		completeBarrier.reset(1);
		guiDrawTask.update(completeBarrier);

		//Update GUI Debug Information
		ScreenDebugInfo.instance.setFrameRate(frameRateTimer.getPerSecond());
		ScreenDebugInfo.instance.setDrawInfo(
				worldDrawTask.drawViewCount,
				worldDrawTask.drawShadowCount,
				worldDrawTask.drawNearbyCount,
				worldDrawTask.updateCount
		);

		//Submit the Async Task
		renderTaskPool.addWork(guiDrawTask);
	}

	/**
	 * Asynchronously generate command buffers
	 *  & then submit the frames to the GPU to present
	 */
	public void submitFrame(@NotNull AsyncBarrier barrier) {
		UsageAnalyses.StartCPUSample("API Submit",0);
		boolean success = api.submitNextFrame(renderTaskPool,barrier,worldDrawTask);
		UsageAnalyses.StopCPUSample();
		if(!success) {
			changeStateRequest = true;
		}
		barrier.awaitCompletion();
	}

}
