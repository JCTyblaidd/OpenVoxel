package net.openvoxel.client.renderer.vk;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.generic.DisplayHandle;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWaitEventsTimeout;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

/**
 * Created by James on 01/09/2016.
 *
 * Vulkan Display Handle
 */
public class VKDisplayHandle implements DisplayHandle{

	private long window;

	VKDisplayHandle(long window) {
		this.window = window;
	}

	@Override
	public void pollEvents() {
		glfwWaitEventsTimeout(1.0 / 60.0);
		if(glfwWindowShouldClose(window)) {
			OpenVoxel.getInstance().AttemptShutdownSequence(false);
		}
		//TODO: hook to main handle
	}

	@Override
	public int getRefreshRate() {
		return 0;
	}

	@Override
	public void setRefreshRate(int hz) {

	}
}
