package net.openvoxel.client.renderer.gl3.worldrender.shader;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

/**
 * Created by James on 28/09/2016.
 *
 * Uniform Buffer Information
 */
public class OGL3World_UniformCache {


	public static void Load() {
		int[] arr = new int[5];
		glGenBuffers(arr);
		UBO_Settings = arr[0];
		UBO_FinalFrame = arr[1];
		UBO_ChunkConstants = arr[2];
		UBO_TextureAtlas = arr[3];
		UBO_ShadowMap = arr[4];
		buf_settings = MemoryUtil.memAlloc(0);
		buf_final_frame = MemoryUtil.memAlloc(4);
		buf_chunk_constants = MemoryUtil.memAlloc(16 * 4);//Mat4,
		buf_texture_atlas = MemoryUtil.memAlloc(3);
		buf_shadow_map = MemoryUtil.memAlloc(4);
	}

	public void updateSettings() {
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_Settings);
		glBufferData(GL_UNIFORM_BUFFER,buf_settings,GL_DYNAMIC_DRAW);
	}

	public void updateFinalFrame() {
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_FinalFrame);
		glBufferData(GL_UNIFORM_BUFFER, buf_final_frame,GL_DYNAMIC_DRAW);
	}

	public void updateChunkConstants() {
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_ChunkConstants);
		glBufferData(GL_UNIFORM_BUFFER,buf_chunk_constants,GL_DYNAMIC_DRAW);
	}

	public void updateTextureAtlas() {
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_TextureAtlas);
		glBufferData(GL_UNIFORM_BUFFER,buf_texture_atlas,GL_DYNAMIC_DRAW);
	}

	public void updateShadowMap() {
		glBindBuffer(GL_UNIFORM_BUFFER,UBO_ShadowMap);
		glBufferData(GL_UNIFORM_BUFFER,buf_shadow_map,GL_DYNAMIC_DRAW);
	}

	public static int UBO_Settings, UBO_FinalFrame, UBO_ChunkConstants, UBO_TextureAtlas, UBO_ShadowMap;
	public static ByteBuffer buf_settings, buf_final_frame, buf_chunk_constants, buf_texture_atlas, buf_shadow_map;

}
