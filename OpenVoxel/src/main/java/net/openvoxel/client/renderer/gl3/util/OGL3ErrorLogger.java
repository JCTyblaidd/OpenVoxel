package net.openvoxel.client.renderer.gl3.util;

import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL43.*;

/**
 * Created by James on 05/08/2016.
 *
 * OpenGL Error Logger
 */
public class OGL3ErrorLogger extends GLDebugMessageCallback{

	public static void Handle() {
		if(GL.getCapabilities().glDebugMessageCallback != 0) {
			glEnable(GL_DEBUG_CALLBACK_FUNCTION);
			glDebugMessageCallback(new OGL3ErrorLogger(), 0);
			OGL3Renderer.gl3Log.Info("Enabled: GL3 Debug Callback");
		}else{
			OGL3Renderer.gl3Log.Warning("Failure to enable GL3 Debug");
		}
	}

	private String _getSrc(int src) {
		switch (src) {
			case GL_DEBUG_SOURCE_API:
				return "API";
			case GL_DEBUG_SOURCE_APPLICATION:
				return "Application";
			case GL_DEBUG_SOURCE_OTHER:
				return "Other";
			case GL_DEBUG_SOURCE_SHADER_COMPILER:
				return "Shader Compiler";
			case GL_DEBUG_SOURCE_THIRD_PARTY:
				return "Third Party";
			case GL_DEBUG_SOURCE_WINDOW_SYSTEM:
				return "Window System";
			default:
				return "Unknown";
		}
	}
	private String _getType(int type) {
		switch(type) {
			case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
				return "Deprecated Behaviour";
			case GL_DEBUG_TYPE_ERROR:
				return "Error";
			case GL_DEBUG_TYPE_MARKER:
				return "Marker";
			case GL_DEBUG_TYPE_OTHER:
				return "Other";
			case GL_DEBUG_TYPE_PERFORMANCE:
				return "Performance";
			case GL_DEBUG_TYPE_POP_GROUP:
				return "Pop Group";
			case GL_DEBUG_TYPE_PORTABILITY:
				return "Portability";
			case GL_DEBUG_TYPE_PUSH_GROUP:
				return "Push Group";
			case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
				return "Undefined Behaviour";
			default:
				return "Unknown";
		}
	}


	@Override
	public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
		if(source == GL_DEBUG_SOURCE_API && type == GL_DEBUG_TYPE_OTHER) return;//SKIP//
		String source_str = _getSrc(source);
		String type_str = _getType(type);
		String content = getMessage(length,message);

		String output = "["+source_str+"] ["+type_str+"]: \n\t\t";
		output += content.replace("\n","\n\t\t");
		switch(severity) {
			case GL_DEBUG_SEVERITY_NOTIFICATION:
				OGL3Renderer.gl3Log.Debug(output);
				break;
			case GL_DEBUG_SEVERITY_LOW:
				OGL3Renderer.gl3Log.Info(output);
				break;
			case GL_DEBUG_SEVERITY_MEDIUM:
				OGL3Renderer.gl3Log.Warning(output);
				break;
			case GL_DEBUG_SEVERITY_HIGH:
				OGL3Renderer.gl3Log.Severe(output);
				break;
		}
	}
}
