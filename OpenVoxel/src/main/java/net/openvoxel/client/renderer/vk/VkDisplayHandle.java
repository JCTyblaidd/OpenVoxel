package net.openvoxel.client.renderer.vk;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;

import static org.lwjgl.glfw.GLFW.glfwWaitEventsTimeout;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

/**
 * Created by James on 01/09/2016.
 *
 * Vulkan Display Handle
 */
public class VkDisplayHandle implements DisplayHandle{

	private VkDeviceState state;

	VkDisplayHandle(VkDeviceState deviceState) {
		state = deviceState;
	}

	@Override
	public void pollEvents() {
		glfwWaitEventsTimeout(1.0 / 60.0);
		if(glfwWindowShouldClose(state.glfw_window)) {
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
