package net.openvoxel.api.logger;

import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by James on 04/09/2016.
 *
 * LogCallback for GLFW
 */
public class GLFWLogWrapper extends GLFWErrorCallback{

	public static GLFWLogWrapper INSTANCE;
	private static GLFWLogWrapper get() {
		if(INSTANCE == null) {
			INSTANCE = new GLFWLogWrapper();
		}
		return INSTANCE;
	}

	private Logger glfwLog;
	private GLFWLogWrapper() {
		glfwLog = Logger.getLogger("GLFW");
	}


	public static void Load() {
		glfwSetErrorCallback(get());
	}

	public static void Unload() {
		if(INSTANCE != null) {
			INSTANCE.free();
			INSTANCE = null;
		}
	}

	private static String getErr(int err) {
		switch (err) {
			case GLFW_NOT_INITIALIZED:
				return "Not Initialized";
			case GLFW_NO_CURRENT_CONTEXT:
				return "No Current Context";
			case GLFW_INVALID_ENUM:
				return "Invalid Enum";
			case GLFW_INVALID_VALUE:
				return "Invalid Value";
			case GLFW_OUT_OF_MEMORY:
				return "Out Of Memory";
			case GLFW_API_UNAVAILABLE:
				return "API Unavailable";
			case GLFW_VERSION_UNAVAILABLE:
				return "Version Unavailable";
			case GLFW_PLATFORM_ERROR:
				return "Platform Error";
			case GLFW_FORMAT_UNAVAILABLE:
				return "Format Unavailable";
			case GLFW_NO_WINDOW_CONTEXT:
				return "No Window Context";
			default:
				return "Unknown Error: " + err;
		}
	}

	@Override
	public void invoke(int error, long description) {
		String err = getErr(error);
		String data = getDescription(description);
		glfwLog.Severe(err + " : " + data);
	}
}
