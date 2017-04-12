package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

import net.openvoxel.client.renderer.gl3.OGL3Renderer;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Created by James on 11/04/2017.
 *
 * Handles Realistic Global Illumination for areas close to the player
 */
class OGL3NearGlobalIlluminationHandler {

	private OGL3DeferredWorldRenderer deferredWorldRenderer;

	private int voxelRenderTarget;

	//TODO:? do we store pure col or col w/ texture value
	//TODO:? cascade the voxel information?
	//Default: 16x16x16 per block with 64x64x64 per area >>> 4096
	private int voxelAreaSize = 2;//4096; //todo: impl

	OGL3NearGlobalIlluminationHandler(OGL3DeferredWorldRenderer deferredWorldRenderer) {
		this.deferredWorldRenderer = deferredWorldRenderer;
		//init_voxel_target();
	}

	private void init_voxel_target() {
		voxelRenderTarget = glGenTextures();
		glActiveTexture(GL_TEXTURE0 + OGL3Renderer.TextureBinding_NearVoxelMap);
		glBindTexture(GL_TEXTURE_3D,voxelRenderTarget);
		int size = voxelAreaSize;
		glTexImage3D(GL_TEXTURE_3D,0,GL_RGBA8,size,size,size,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);
		glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
		glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
		glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_WRAP_R,GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
	}

	private void resize_voxel_target() {

	}

	public void updateUniformInfo() {

	}

	public void renderToVoxels() {

	}
}
