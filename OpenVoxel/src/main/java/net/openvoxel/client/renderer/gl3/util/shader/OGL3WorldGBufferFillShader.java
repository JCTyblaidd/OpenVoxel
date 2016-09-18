package net.openvoxel.client.renderer.gl3.util.shader;

import net.openvoxel.client.renderer.gl3.util.OGL3BasicShader;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;

/**
 * Created by James on 25/08/2016.
 */
public class OGL3WorldGBufferFillShader extends OGL3BasicShader {

	private int tex_diff_source;

	public OGL3WorldGBufferFillShader(String shaderSource) {
		super(shaderSource, "worldGBufferFillShader");
		//Load Uniforms//
		tex_diff_source = glGetUniformLocation(program_ID,"textureAtlasDiff");
	}


}
