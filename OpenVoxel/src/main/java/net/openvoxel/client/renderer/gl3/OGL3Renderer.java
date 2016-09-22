package net.openvoxel.client.renderer.gl3;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.gui.ScreenDebugInfo;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.generic.GlobalRenderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.generic.config.RenderConfig;
import net.openvoxel.client.renderer.gl3.font.OGL3FontRenderer;
import net.openvoxel.client.renderer.gl3.util.OGL3ErrorLogger;
import net.openvoxel.client.renderer.gl3.worldrender.GL_Caps;
import net.openvoxel.common.event.input.WindowResizeEvent;
import net.openvoxel.files.FolderUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GLCapabilities;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;

/**
 * Created by James on 25/08/2016.
 *
 * OpenGL 3.3 Implementation Of The Renderer
 */
public class OGL3Renderer implements GlobalRenderer{

	private long window;

	public static OGL3Renderer instance;
	public static Logger gl3Log = Logger.getLogger("GL3");
	private OGL3WorldRenderer worldRenderer;
	private OGL3DisplayHandle displayHandle;
	private OGL3GUIRenderer guiRenderer;
	private RenderConfig settingChangeRequested;

	WindowResizeEvent windowResized;
	volatile boolean stateChangeRequested = false;
	volatile boolean stateRequestedFullscreen = false;
	volatile int requestedRefreshRate = 60;
	volatile boolean screenshotRequested = false;

	public OGL3Renderer() {
		instance = this;
		settingChangeRequested = new RenderConfig();
	}

	@Override
	public String getShaderPostfix() {
		return "glsl";
	}

	@Override
	public void requestSettingsChange(RenderConfig newConfig) {
		settingChangeRequested = newConfig;
	}

	@Override
	public WorldRenderer getWorldRenderer() {
		return worldRenderer;
	}

	@Override
	public DisplayHandle getDisplayHandle() {
		return displayHandle;
	}

	@Override
	public GUIRenderer getGUIRenderer() {
		return guiRenderer;
	}
	private int oldWidth = ClientInput.currentWindowWidth;
	private int oldHeight = ClientInput.currentWindowHeight;
	private AtomicBoolean stateChangeLock = new AtomicBoolean(false);
	private void updateFullScreenState() {
		int newWidth;
		int newHeight;
		long currentMonitor = glfwGetPrimaryMonitor();
		if (stateRequestedFullscreen)
		{
			GLFWVidMode vidMode = glfwGetVideoMode(currentMonitor);

			oldWidth = ClientInput.currentWindowWidth;
			oldHeight = ClientInput.currentWindowHeight;
			newWidth = vidMode.width();
			newHeight = vidMode.height();
		}
		else
		{
			newWidth = oldWidth;
			newHeight = oldHeight;
		}
		glfwSetWindowMonitor(window, stateRequestedFullscreen ? currentMonitor : 0,0,0,newWidth,newHeight,requestedRefreshRate);
	}


	@Override
	public void loadPreRenderThread() {
		boolean glDebug = OpenVoxel.getLaunchParameters().hasFlag("glDebug");
		requestedRefreshRate = glfwGetVideoMode(glfwGetPrimaryMonitor()).refreshRate();
		gl3Log.Info("Refresh Rate Defaulted: " + requestedRefreshRate);
		try {
			glfwDefaultWindowHints();//Setup OpenGL Profile w/ Version >= 3.3
			glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
			glfwWindowHint(GLFW_REFRESH_RATE,requestedRefreshRate);
			if(glDebug) {
				glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT,GLFW_TRUE);
			}
			window = glfwCreateWindow(ClientInput.currentWindowWidth, ClientInput.currentWindowHeight, "Open Voxel " + OpenVoxel.currentVersion.getValString(), 0, 0);
		}catch(Throwable e) {
			gl3Log.Severe("Error Creating Screen");
			gl3Log.StackTrace(e);
			System.exit(-1);
		}
		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);//Enable VSync : Disabled -> Refresh Rate Used Instead//
		createCapabilities(true);
		glViewport(0,0,ClientInput.currentWindowWidth,ClientInput.currentWindowHeight);
		glEnable (GL_CULL_FACE); // cull face
		glCullFace (GL_BACK); // cull back face
		glFrontFace (GL_CW); //clockwise
		guiRenderer = new OGL3GUIRenderer();
		worldRenderer = new OGL3WorldRenderer();
		displayHandle = new OGL3DisplayHandle(window,this);
		updateDebug();
		OGL3FontRenderer.Init();
		GL_Caps.Load();
		if(glDebug) {
			OGL3ErrorLogger.Handle();
		}
		glClearColor(0,0,0,1);
		glfwMaximizeWindow(window);
		glfwMakeContextCurrent(0);
		capabilities = getCapabilities();
	}

	private void updateDebug() {
		int maj = glGetInteger(GL_MAJOR_VERSION);
		int min = glGetInteger(GL_MINOR_VERSION);
		String vendorString = glGetString(GL_VENDOR);
		String rendererName = glGetString(GL_RENDERER);
		ScreenDebugInfo.RendererType = "OpenGL "+maj+"."+min;
		ScreenDebugInfo.RendererDriver = rendererName;
		ScreenDebugInfo.RendererVendor = vendorString;
	}

	private GLCapabilities capabilities;

	@Override
	public void loadPostRenderThread() {
		glfwMakeContextCurrent(window);
		setCapabilities(capabilities);
	}

	@Override
	public void nextFrame() {
		if(screenshotRequested) {
			screenshotRequested = false;
			final int W = ClientInput.currentWindowWidth;
			final int H = ClientInput.currentWindowHeight;
			int[] pixels = new int[W * H];
			glReadPixels(0,0,W,H,GL_BGRA,GL_UNSIGNED_BYTE,pixels);
			FolderUtils.saveScreenshot(W,H,pixels);
		}
		//Next Frame//
		glfwSwapBuffers(window);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		if(settingChangeRequested != null) {
			worldRenderer.onSettingsChanged(settingChangeRequested);
			settingChangeRequested = null;
		}
		if(windowResized != null) {
			glViewport(0,0,windowResized.Width,windowResized.Height);
			windowResized = null;
		}
		if(stateChangeRequested) {
			stateChangeLock.set(true);
			while(stateChangeLock.get()) {//Await Change On Main Thread//
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {}
			}
			loadPostRenderThread();
		}
	}

	public void pollHooks() {
		if(stateChangeRequested) {
			if(stateChangeLock.get()) {
				updateFullScreenState();
				glfwMakeContextCurrent(0);
				stateChangeRequested = false;
				stateChangeLock.set(false);
			}
		}
	}

	@Override
	public void kill() {
		glfwDestroyWindow(window);
	}
}
