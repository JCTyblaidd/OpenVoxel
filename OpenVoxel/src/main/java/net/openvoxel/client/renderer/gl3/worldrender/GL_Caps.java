package net.openvoxel.client.renderer.gl3.worldrender;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Created by James on 11/09/2016.
 *
 * Information About OpenGL Capabilities: if advanced optimization = true
 */
public class GL_Caps {

	public static void Load() {}
	static {
		GLCapabilities caps = GL.getCapabilities();
		CanPerformSinglePassCascade = true;
		CanPerformSinglePassBlockData = true;
		CanUseASTCCompression = caps.GL_KHR_texture_compression_astc_ldr;
	}

	/**
	 * Use ASTC Texture Compression instead of default GL Compression
	 */
	public static final boolean CanUseASTCCompression;

	public static final boolean CanPerformSinglePassCascade;

	public static final boolean CanPerformSinglePassBlockData;

}
