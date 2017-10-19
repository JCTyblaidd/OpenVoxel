package net.openvoxel.client.renderer.vk;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.glfw.GLFWEventHandler;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.SubscribeEvents;
import net.openvoxel.common.event.input.KeyStateChangeEvent;
import net.openvoxel.common.event.input.WindowResizeEvent;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 01/09/2016.
 *
 * Vulkan Display Handle
 */
public class VkDisplayHandle implements DisplayHandle, EventListener {

	private VkDeviceState state;
	private VkRenderer renderer;

	VkDisplayHandle(VkRenderer renderer,VkDeviceState deviceState) {
		state = deviceState;
		GLFWEventHandler.Load(state.glfw_window);
	}

	void cleanup() {
		GLFWEventHandler.Unload();
	}

	@SubscribeEvents
	public void windowResize(WindowResizeEvent event) {
		renderer.markAsResizeRequired();
	}

	@SubscribeEvents
	public void buttonPressed(KeyStateChangeEvent e) {
		if (e.GLFW_KEY == GLFW_KEY_F11 && e.GLFW_KEY_STATE == GLFW_PRESS) {
			//state.toggleFullScreenRequest();
		}
		if (e.GLFW_KEY == GLFW_KEY_F12 && e.GLFW_KEY_STATE == GLFW_PRESS) {
			//state.getScreenshotRequest();
		}
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
