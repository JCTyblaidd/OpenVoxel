package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

import net.openvoxel.client.ClientInput;
import net.openvoxel.world.client.ClientChunkSection;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengles.GLES20.*;

/**
 * Created by James on 10/04/2017.
 *
 * Deferred World Rendering Instance
 */
public class OGL3DeferredWorldRenderer {

	//////////////////////
	/// Render Targets ///
	//////////////////////

	//GBuffer Draw Information//
	private int gBuffer_Diffuse;
	private int gBuffer_UV;
	private int gBuffer_Normal;
	private int gBuffer_Lighting;
	private int gBuffer_Depth;

	//Shadow Pass Information
	//TODO: convert to array
	private int shadowPass1_Depth;
	private int shadowPass2_Depth;
	private int shadowPass3_Depth;

	//Merge to run post target//
	private int prepost_Colour;

	//FrameBuffer Target//
	private int frameBufferTarget_GBuffer;
	private int frameBufferTarget_Shadows;
	private int frameBufferTarget_Post;


	private List<ClientChunkSection> cullViewport;
	private List<ClientChunkSection> cullShadow;

	private void initialize() {
		frameBufferTarget_GBuffer = glGenFramebuffers();
		frameBufferTarget_Shadows = glGenFramebuffers();
		frameBufferTarget_Post = glGenFramebuffers();
		gBuffer_Diffuse = glGenTextures();
		gBuffer_UV = glGenTextures();
		gBuffer_Normal = glGenTextures();
		gBuffer_Lighting = glGenTextures();
		gBuffer_Depth = glGenTextures();
		shadowPass1_Depth = glGenTextures();
		shadowPass2_Depth = glGenTextures();
		shadowPass3_Depth = glGenTextures();
		prepost_Colour = glGenTextures();
		//Init GBuffer//
		update_textures(ClientInput.currentWindowWidth.get(),ClientInput.currentWindowHeight.get());
	}
	private void update_textures(int width, int height) {
		glActiveTexture(GL_TEXTURE0);
		glBindBuffer(GL_FRAMEBUFFER,frameBufferTarget_GBuffer);
		glBindTexture(GL_TEXTURE_2D,gBuffer_Diffuse);
		glTexImage2D(GL_TEXTURE_2D,0,GL_RGB,width,height,0,GL_RGB,GL_UNSIGNED_BYTE,(ByteBuffer)null);

	}

	private void setupRenderTargetGBuffer() {
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_GBuffer);
	}

	private void setupRenderTargetShadows() {
		//glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_Shadows);
	}

	private void setupRenderTargetMerge() {
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_Post);
	}

	private void setupRenderTargetFinal() {
		glBindFramebuffer(GL_FRAMEBUFFER,0);
	}
}
