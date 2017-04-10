package net.openvoxel.client.renderer.glfw;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.common.event.input.*;
import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 01/08/2016.
 */
public class GLFWEventHandler {

	public static GLFWEventHandler INSTANCE;

	public static void Load(long window) {
		INSTANCE = new GLFWEventHandler(window);
	}

	public GLFWEventHandler(long window) {
		glfwSetCursorPosCallback(window,new CursorPosCallback());
		glfwSetKeyCallback(window,new KeyCallback());
		glfwSetCharCallback(window,new CharacterCallback());
		glfwSetMouseButtonCallback(window,new CursorButtonCallback());
		glfwSetWindowSizeCallback(window, new WindowSizeCallback());
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

	private static class WindowSizeCallback extends GLFWWindowSizeCallback {
		@Override
		public void invoke(long window, int width, int height) {
			OpenVoxel.pushEvent(new WindowResizeEvent(width,height));
			ClientInput.currentWindowHeight.set(height);
			ClientInput.currentWindowWidth.set(width);
		}
	}
}
