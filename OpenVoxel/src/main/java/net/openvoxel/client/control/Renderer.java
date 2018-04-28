package net.openvoxel.client.control;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.old_renderer.generic.GlobalRenderer;
import net.openvoxel.client.old_renderer.gl3.OGL3Renderer;
import net.openvoxel.client.old_renderer.vk.VkRenderer;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.utility.AsyncRunnablePool;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

/**
 * Created by James on 25/08/2016.
 *
 * Main Renderer Wrapper: with reference to the current renderer, main renderer logger and the async work queue
 */
@Deprecated
public class Renderer {

	public static GlobalRenderer renderer;
	public static AsyncRunnablePool renderCacheManager;
	public static Logger logger;

	static {
		//TODO: remove after vulkan renderer is implemented
		//OpenVoxel.getLaunchParameters().storeRuntimeFlag("openGL");
		//Logger.getLogger("SHODDY FIXES").Severe("Prevented Vulkan Renderer From Being Used {Reason: Not Yet Implemented}");
	}

	public static void Initialize() {
		logger = Logger.getLogger("Render Thread");
		glfwInit();
		Logger log = Logger.getLogger("Renderer Init");
		boolean flag_gl = OpenVoxel.getLaunchParameters().hasFlag("openGL");
		boolean flag_vk = OpenVoxel.getLaunchParameters().hasFlag("vulkan");
		boolean vulkan_supported = glfwVulkanSupported();
		if(flag_vk && !vulkan_supported) {
			log.Warning("Vulkan Requested But Not Found");
		}
		if(flag_gl && vulkan_supported) {
			log.Warning("Vulkan Available But OpenGL Requested");
		}
		if(vulkan_supported && !flag_gl){
			log.Info("Loading Vulkan Renderer");
			renderer = new VkRenderer();
		}else {
			log.Info("Loading OGL3 Renderer");
			renderer = new OGL3Renderer();
		}
		renderCacheManager = new AsyncRunnablePool("Renderer Worker Group",AsyncRunnablePool.getWorkerCount("renderWorkerCount",4));
		renderCacheManager.start();
		renderer.loadPreRenderThread();
	}

	public static IconAtlas getBlockTextureAtlas() {
		if(renderer != null) {
			return renderer.getBlockAtlas();
		}else{
			logger.Severe("Attempted to get IconAtlas From Non-Existent Renderer");
			return null;
		}
	}
}
