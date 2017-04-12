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
import net.openvoxel.client.renderer.gl3.atlas.OGL3TextureAtlas;
import net.openvoxel.client.renderer.gl3.font.OGL3FontRenderer;
import net.openvoxel.client.renderer.gl3.util.OGL3ErrorLogger;
import net.openvoxel.client.renderer.gl3.worldrender.OGL_Caps;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_UniformCache;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.event.input.WindowResizeEvent;
import net.openvoxel.files.FolderUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GLCapabilities;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
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
 *
 *
 *
 * Texture Binding Points:
 *
 *
 */
public class OGL3Renderer implements GlobalRenderer{

	public static final int TextureBinding_tDiffuse = 10;
	public static final int TextureBinding_tNormal = 11;
	public static final int TextureBinding_tPBR = 12;
	public static final int TextureBinding_tItemDiffuse = 13;
	public static final int TextureBinding_SkyCubeMap = 14;
	public static final int TextureBinding_Shadows = 15;
	public static final int TextureBinding_GBufferDiffuse = 16;
	public static final int TextureBinding_GBufferNormal = 17;
	public static final int TextureBinding_GBufferPBR = 18;
	public static final int TextureBinding_GBufferLighting = 19;
	public static final int TextureBinding_GBufferDepth = 20;
	public static final int TextureBinding_MergedTextureTarget = 21;
	public static final int TextureBinding_NearVoxelMap = 22;

	public static final int UniformBlockBinding_Settings = 0;
	public static final int UniformBlockBinding_FrameInfo = 1;
	public static final int UniformBlockBinding_ChunkInfo = 2;
	public static final int UniformBlockBinding_ShadowInfo = 3;
	public static final int UniformBlockBinding_VoxelInfo = 4;

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

	public OGL3TextureAtlas blockAtlas;

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
	private int oldWidth = ClientInput.currentWindowWidth.get();
	private int oldHeight = ClientInput.currentWindowHeight.get();
	private AtomicBoolean stateChangeLock = new AtomicBoolean(false);
	private void updateFullScreenState() {
		int newWidth;
		int newHeight;
		long currentMonitor = glfwGetPrimaryMonitor();
		if (stateRequestedFullscreen)
		{
			GLFWVidMode vidMode = glfwGetVideoMode(currentMonitor);

			oldWidth = ClientInput.currentWindowWidth.get();
			oldHeight = ClientInput.currentWindowHeight.get();
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
			window = glfwCreateWindow(ClientInput.currentWindowWidth.get(), ClientInput.currentWindowHeight.get(), "Open Voxel " + OpenVoxel.currentVersion.getValString(), 0, 0);
		}catch(Throwable e) {
			gl3Log.Severe("Error Creating Screen");
			gl3Log.StackTrace(e);
			System.exit(-1);
		}
		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);//Enable VSync : Disabled -> Refresh Rate Used Instead//
		createCapabilities(true);
		glViewport(0,0,ClientInput.currentWindowWidth.get(),ClientInput.currentWindowHeight.get());
		glEnable (GL_CULL_FACE); // cull face
		glCullFace (GL_BACK); // cull back face
		glFrontFace (GL_CW); //clockwise
		guiRenderer = new OGL3GUIRenderer();
		worldRenderer = new OGL3WorldRenderer();
		displayHandle = new OGL3DisplayHandle(window,this);
		blockAtlas = new OGL3TextureAtlas();
		updateDebug();
		OGL3FontRenderer.Init();
		OGL_Caps.Load();
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
			final int W = ClientInput.currentWindowWidth.get();
			final int H = ClientInput.currentWindowHeight.get();
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
			worldRenderer.onWindowResized(windowResized.Width,windowResized.Height);
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

	void pollHooks() {
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
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
		OGL3World_UniformCache.FreeMemory();
	}

	@Override
	public IconAtlas getBlockAtlas() {
		return blockAtlas;
	}
}
