package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import net.openvoxel.client.renderer.gl3.OGL3WorldRenderer;
import net.openvoxel.client.renderer.gl3.worldrender.cache.OGL3RenderCache;
import net.openvoxel.client.renderer.gl3.worldrender.shader.OGL3World_ShaderCache;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

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
	private int gBuffer_PBR;
	private int gBuffer_Normal;
	private int gBuffer_Lighting;
	private int gBuffer_Depth;

	//GBuffer Transparent Information//
	private int gBuffer_Transparent1;
	private int gBuffer_Transparent2;

	//Utilities//
	private OGL3CascadeManager shadowCascades;
	private OGL3NearGlobalIlluminationHandler nearGI;
	private OGL3DeferredCuller culler;

	//Merge to run post target//
	private int mergeTarget_Colour;

	//FrameBuffer Target//
	private int frameBufferTarget_GBuffer;
	private int frameBufferTarget_Post;

	//Full Screen Draw//
	private int fullScreenVAO;
	private int fullScreenBufPos;
	private int fullScreenBufUV;


	private List<ClientChunkSection> cullViewport;
	private List<ClientChunkSection> cullShadow;

	private OGL3WorldRenderer worldRenderer;

	public OGL3DeferredWorldRenderer(OGL3WorldRenderer worldRenderer) {
		this.worldRenderer = worldRenderer;
		shadowCascades = new OGL3CascadeManager();
		nearGI = new OGL3NearGlobalIlluminationHandler(this);
		initialize(ClientInput.currentWindowWidth.get(),ClientInput.currentWindowHeight.get());
	}

	private void initialize(int width, int height) {
		frameBufferTarget_GBuffer = glGenFramebuffers();
		frameBufferTarget_Post = glGenFramebuffers();
		gBuffer_Diffuse = glGenTextures();
		gBuffer_PBR = glGenTextures();
		gBuffer_Normal = glGenTextures();
		gBuffer_Lighting = glGenTextures();
		gBuffer_Depth = glGenTextures();
		mergeTarget_Colour = glGenTextures();
		//Setup Full Screen Render Pass Request//
		fullScreenVAO = glGenVertexArrays();
		fullScreenBufPos = glGenBuffers();
		fullScreenBufUV = glGenBuffers();
		glBindVertexArray(fullScreenVAO);
		glBindBuffer(GL_ARRAY_BUFFER, fullScreenBufPos);
		glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,-1,1,1,1,-1,-1,1,1,1,-1},GL_STATIC_DRAW);
		glVertexAttribPointer(0,2,GL_FLOAT,false,0,0);
		glBindBuffer(GL_ARRAY_BUFFER, fullScreenBufUV);
		glBufferData(GL_ARRAY_BUFFER,new float[]{0 , 0, 0,1,1,1, 0, 0,1,1,1, 0},GL_STATIC_DRAW);
		glVertexAttribPointer(1,2,GL_FLOAT,false,0,0);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glBindVertexArray(0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		update_textures(width, height);
		bind_textures();
		bind_render_targets();
		culler = new OGL3DeferredCuller();
	}

	private void bind_textures() {
		bind_tex(gBuffer_Diffuse, OGL3Renderer.TextureBinding_GBufferDiffuse);
		bind_tex(gBuffer_PBR, OGL3Renderer.TextureBinding_GBufferPBR);
		bind_tex(gBuffer_Normal, OGL3Renderer.TextureBinding_GBufferNormal);
		bind_tex(gBuffer_Lighting, OGL3Renderer.TextureBinding_GBufferLighting);
		bind_tex(gBuffer_Depth, OGL3Renderer.TextureBinding_GBufferDepth);
		//
		bind_tex(mergeTarget_Colour,OGL3Renderer.TextureBinding_MergedTextureTarget);
		glActiveTexture(GL_TEXTURE0);
	}

	private void bind_tex(int id, int binding) {
		glActiveTexture(GL_TEXTURE0+binding);
		glBindTexture(GL_TEXTURE_2D,id);
	}

	private void bind_render_targets() {
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_GBuffer);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, gBuffer_Diffuse, 0);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, gBuffer_PBR, 0);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, gBuffer_Normal, 0);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, gBuffer_Lighting, 0);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT , GL_TEXTURE_2D, gBuffer_Depth,0);
		glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3});
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
			OGL3Renderer.gl3Log.Severe("GBuffer Target is Not Complete!!!");
			OGL3Renderer.gl3Log.Severe("err code = "+glCheckFramebufferStatus(GL_FRAMEBUFFER));
			System.exit(-1);
		}
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_Post);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mergeTarget_Colour, 0);
		glDrawBuffer(GL_COLOR_ATTACHMENT0);
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
			OGL3Renderer.gl3Log.Severe("GBuffer Target is Not Complete!!!");
			OGL3Renderer.gl3Log.Severe("err code = "+glCheckFramebufferStatus(GL_FRAMEBUFFER));
			System.exit(-1);
		}
		glBindFramebuffer(GL_FRAMEBUFFER,0);
	}

	private void cleanup_delete() {
		glDeleteFramebuffers(new int[]{frameBufferTarget_GBuffer,frameBufferTarget_Post});
		glDeleteTextures(new int[]{gBuffer_Diffuse,gBuffer_PBR,gBuffer_Normal,gBuffer_Lighting,gBuffer_Depth});
	}

	private void drawFullScreen() {
		glBindVertexArray(fullScreenVAO);
		glDrawArrays(GL_TRIANGLES,0,6);
	}

	private void set_tex(int width,int height,int tex,int type) {
		glBindTexture(GL_TEXTURE_2D,tex);
		glTexImage2D(GL_TEXTURE_2D,0,type,width,height,0,GL_RGB,GL_UNSIGNED_BYTE,(ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
	}

	private void set_depth(int width, int height, int tex, int type) {
		glBindTexture(GL_TEXTURE_2D,tex);
		glTexImage2D(GL_TEXTURE_2D,0,type,width,height,0,GL_DEPTH_COMPONENT,GL_FLOAT,(ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
	}



	private void update_textures(int width, int height) {
		glActiveTexture(GL_TEXTURE0);
		//UPDATE GBuffer Information//
		set_tex(width,height,gBuffer_Diffuse,GL_RGB);
		set_tex(width,height,gBuffer_PBR,GL_RGBA);
		set_tex(width,height,gBuffer_Normal,GL_RGB);
		set_tex(width,height,gBuffer_Lighting,GL_RGBA);
		set_depth(width,height,gBuffer_Depth,GL_DEPTH_COMPONENT);
		//UPDATE PrePost Information//
		set_tex(width,height, mergeTarget_Colour,GL_RGBA);
		//Update Sizes//
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_GBuffer);
		glViewport(0,0,width,height);
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_Post);
		glViewport(0,0,width,height);
		glBindFramebuffer(GL_FRAMEBUFFER,0);
	}

	private void setupRenderTargetGBuffer() {
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_GBuffer);
		glClearColor(1,0,0,1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glDisable(GL_BLEND);
	}

	private void setupRenderTargetTransparentGBuffer() {

	}
	private void setupRenderTargetMerge() {
		glBindFramebuffer(GL_FRAMEBUFFER,frameBufferTarget_Post);
		glClearColor(0,0,0,1);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
	}

	private void setupRenderTargetFinal() {
		glBindFramebuffer(GL_FRAMEBUFFER,0);
		glDisable(GL_DEPTH_TEST);
	}

	public void preRenderWorld(EntityPlayerSP player,Set<ClientChunk> toRender) {
		culler.startCulling(player,toRender);
	}

	public void renderWorld(EntityPlayerSP player, ClientWorld world) {
		//Draw Standard Output
		setupRenderTargetGBuffer();
		OGL3World_ShaderCache.GBUFFER_OPAQUE.use();
		List<OGL3RenderCache> standardCull = culler.requestCullStandard();
		for(OGL3RenderCache cache : standardCull) {
			if(cache.cacheExists()) {
				worldRenderer.setupCacheUniform(cache.chunk,cache.yPos);
				cache.draw();
			}
		}
		//Draw FoV Opaque
		//setupRenderTargetTransparentGBuffer();
		//for(OGL3RenderCache cache : standardCull) {
		//  if(cache.cacheExists()) {
		//      worldRenderer.setupCacheUniform(cache.chunk,cache.yPos);
		//      cache.draw();
		//  }
		//}

		//Draw ShadowMap//
		//setupRenderTarget


		///FINISH THE RENDER PASS///
		setupRenderTargetMerge();
		OGL3World_ShaderCache.GBUFFER_MERGE.use();
		drawFullScreen();
		setupRenderTargetFinal();
		OGL3World_ShaderCache.WORLD_POSTPROCESS.use();
		drawFullScreen();
	}

	public void onFrameResize(int newWidth, int newHeight) {
		cleanup_delete();
		initialize(newWidth,newHeight);
	}

	public void updateUniforms() {
		shadowCascades.updateShadowInfoUniform();
	}
}
