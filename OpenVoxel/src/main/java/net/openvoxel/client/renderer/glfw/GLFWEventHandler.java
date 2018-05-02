package net.openvoxel.client.renderer.glfw;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.common.event.input.*;
import net.openvoxel.common.event.window.WindowCloseRequestedEvent;
import org.lwjgl.glfw.*;
import org.lwjgl.system.Callback;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 01/08/2016.
 *
 * Forwards events from the window to the event bus
 */
public class GLFWEventHandler {

	public static GLFWEventHandler INSTANCE;

	public static void Load(long window) {
		INSTANCE = new GLFWEventHandler(window);
	}

	private List<Callback> callbacks = new ArrayList<>();

	private <T extends Callback> T _register(T t) {
		callbacks.add(t);
		return t;
	}

	public static void Unload() {
		INSTANCE.callbacks.forEach(Callback::free);
	}

	private GLFWEventHandler(long window) {
		glfwSetCursorPosCallback(window,_register(new CursorPosCallback()));
		glfwSetKeyCallback(window,_register(new KeyCallback()));
		glfwSetCharCallback(window,_register(new CharacterCallback()));
		glfwSetMouseButtonCallback(window,_register(new CursorButtonCallback()));
		glfwSetFramebufferSizeCallback(window,_register(new FrameSizeCallback()));
		glfwSetWindowPosCallback(window,_register(new WindowMoveCallback()));
		glfwSetWindowCloseCallback(window,_register(new WindowCloseCallback()));
	}

	private static class CursorPosCallback extends GLFWCursorPosCallback {
		@Override
		public void invoke(long window, double xpos, double ypos) {
			OpenVoxel.pushEvent(new CursorPositionChangeEvent((float)xpos,(float)ypos));
			ClientInput.unhandledMouseDelta.x += xpos - ClientInput.mousePosition.x;
			ClientInput.unhandledMouseDelta.y += ypos - ClientInput.mousePosition.y;
			ClientInput.mousePosition.x = xpos;
			ClientInput.mousePosition.y = ypos;
		}
	}
	private static class KeyCallback extends GLFWKeyCallback {
		@Override
		public void invoke(long window, int key, int scancode, int action, int mods) {
			OpenVoxel.pushEvent(new KeyStateChangeEvent(key,action));
			ClientInput.currentInputStatus[key] = action != GLFW_RELEASE;
		}
	}
	private static class CharacterCallback extends GLFWCharCallback {
		@Override
		public void invoke(long window, int codepoint) {
			OpenVoxel.pushEvent(new CharacterTypedEvent((char)codepoint));
		}
	}

	private static class CursorButtonCallback extends GLFWMouseButtonCallback {
		@Override
		public void invoke(long window, int button, int action, int mods) {
			OpenVoxel.pushEvent(new MouseButtonChangeEvent(button,action!=GLFW_RELEASE));
			ClientInput.currentInputStatus[button] = action != GLFW_RELEASE;
		}
	}

	private static class FrameSizeCallback extends GLFWFramebufferSizeCallback {
		@Override
		public void invoke(long window, int width, int height) {
			OpenVoxel.pushEvent(new WindowResizeEvent(width,height));
			ClientInput.currentWindowFrameSize.set(width,height);
		}
	}

	private static class WindowMoveCallback extends GLFWWindowPosCallback {
		@Override
		public void invoke(long window, int x, int y) {
			//TODO: PUSH EVENT
			ClientInput.currentWindowLocation.set(x,y);
		}
	}

	private static class WindowCloseCallback extends GLFWWindowCloseCallback {
		@Override
		public void invoke(long window) {
			OpenVoxel.pushEvent(new WindowCloseRequestedEvent());
		}
	}
}
