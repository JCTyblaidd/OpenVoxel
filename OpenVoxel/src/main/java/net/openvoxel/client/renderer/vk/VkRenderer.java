package net.openvoxel.client.renderer.vk;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.textureatlas.IconAtlas;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 28/08/2016.
 */
public class VkRenderer implements GlobalRenderer {

	public static Logger vkLog = Logger.getLogger("Vulkan");
	public static VkRenderer Vkrenderer;

	private long window;

	private VKDisplayHandle displayHandle;

	public VkRenderer() {
		vkLog.Severe("Vulkan Renderer Not Implemented");
		System.exit(-1);
		Vkrenderer = this;
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
		vkLog.Info("Loading Pre-Render Thread");
		try {
			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_CLIENT_API,GLFW_NO_API);
			window = glfwCreateWindow(ClientInput.currentWindowWidth, ClientInput.currentWindowHeight, "Open Voxel " + OpenVoxel.currentVersion.getValString(), 0, 0);
		}catch(Throwable e) {
			vkLog.Severe("Error Creating Screen");
			vkLog.StackTrace(e);
			System.exit(-1);
		}
		//Begin Init//
		displayHandle = new VKDisplayHandle(window);
	}

	@Override
	public void loadPostRenderThread() {

	}

	@Override
	public String getShaderPostfix() {
		return "spiv";
	}

	@Override
	public void nextFrame() {
		// TODO: 01/09/2016 Await Frame Finish 
		displayHandle.nextFrame();
	}

	@Override
	public void kill() {

	}

	@Override
	public IconAtlas getBlockAtlas() {
		return null;
	}
}
