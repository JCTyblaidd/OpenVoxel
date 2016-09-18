package net.openvoxel.client.renderer.gl3;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.glfw.GLFWEventHandler;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.SubscribeEvents;
import net.openvoxel.common.event.input.KeyStateChangeEvent;
import net.openvoxel.common.event.input.WindowResizeEvent;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 25/08/2016.
 *
 * Handle Display Information:
 */
public class OGL3DisplayHandle implements DisplayHandle, EventListener{

	private long window;
	private OGL3Renderer Renderer;

	public OGL3DisplayHandle(long w,OGL3Renderer ref) {
		window = w;
		GLFWEventHandler.Load(window);
		OpenVoxel.registerEvents(this);
		Renderer = ref;
	}

	public void changeWindow(long w) {
		window = w;
	}

	@SubscribeEvents
	public void windowResize(WindowResizeEvent E) {
		Renderer.windowResized = E;
	}

	@SubscribeEvents
	public void buttonPressed(KeyStateChangeEvent e) {
		if(e.GLFW_KEY == GLFW_KEY_F11 && e.GLFW_KEY_STATE == GLFW_PRESS) {
			//FullScreen Mode Swap Requested
			Renderer.stateChangeRequested = true;
			Renderer.stateRequestedFullscreen = !Renderer.stateRequestedFullscreen;
		}
		if(e.GLFW_KEY == GLFW_KEY_F12 && e.GLFW_KEY_STATE == GLFW_PRESS) {
			Renderer.screenshotRequested = true;
		}
	}

	@Override
	public void pollEvents() {
		glfwPollEvents();
		if(glfwWindowShouldClose(window)) {
			OpenVoxel.getInstance().AttemptShutdownSequence(false);
		}
		Renderer.pollHooks();
	}

	@Override
	public int getRefreshRate() {
		return Renderer.requestedRefreshRate;
	}

	@Override
	public void setRefreshRate(int hz) {
		Renderer.requestedRefreshRate = hz;
		Renderer.stateChangeRequested = true;//State Changed: Let It Get Handled
	}
}
