package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.generic.DisplayHandle;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;

/**
 * Created by James on 01/09/2016.
 */
public class VKDisplayHandle implements DisplayHandle{

	private long window;

	public VKDisplayHandle(long window) {
		this.window = window;
	}

	public void nextFrame() {

	}

	@Override
	public void pollEvents() {
		glfwPollEvents();
	}

	@Override
	public int getRefreshRate() {
		return 0;
	}

	@Override
	public void setRefreshRate(int hz) {

	}
}
