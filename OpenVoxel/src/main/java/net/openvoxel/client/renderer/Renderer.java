package net.openvoxel.client.renderer;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.renderer.vk.VulkanRenderer;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.AsyncBarrier;
import net.openvoxel.utility.CrashReport;

import java.io.Closeable;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

/**
 * Wrapper around the rendering api that is going to be used
 */
public final class Renderer {

	private final Logger logger;
	private final GraphicsAPI api;

	public Renderer() {
		logger = Logger.getLogger("Renderer");
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
	}

	public void pollInputs() {
		glfwPollEvents();
	}

	public void generateUpdatedChunks(ClientServer server) {

	}

	public void invalidateAllChunks() {

	}

	public void stitchAtlas() {

	}

	/**
	 * Prepare GPU for Streaming of data
	 *
	 * Present Image, vk
	 */
	public void prepareGPU() {
		api.acquireNextFrame();
	}

	/**
	 * Asynchronously Draw the GUI in another thread
	 */
	public void startAsyncGUIDraw(AsyncBarrier completeBarrier) {

	}

	/**
	 * Asynchronously generate command buffers
	 *  & then submit the frames to the GPU to present
	 */
	public void submitToGPU() {
		//Start ASYNC Memory Updating//



		api.submitNextFrame();
	}

}
