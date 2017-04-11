package net.openvoxel.client.renderer.gl3.worldrender.shader;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import net.openvoxel.client.renderer.gl3.util.OGL3BasicShader;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * Created by James on 11/09/2016.
 *
 * World Shader w/ append
 */
public class OGL3WorldAppendedShader extends OGL3BasicShader{

	/**
	 * The Uniform Block for Inserting
	 */
	private static final ResourceHandle shaderPreResource;
	static {
		shaderPreResource = ResourceManager.getResource(ResourceType.SHADER,"world/worldShaderPre");
	}

	OGL3WorldAppendedShader(String shaderSource, String debugID) {
		super(shaderSource, debugID);
		glUseProgram(program_ID);
		_bindUBO("Settings",OGL3Renderer.UniformBlockBinding_Settings);
		_bindUBO("FinalFrame",OGL3Renderer.UniformBlockBinding_FrameInfo);
		_bindUBO("ChunkConstants",OGL3Renderer.UniformBlockBinding_ChunkInfo);
		_bindUBO("ShadowMapping",OGL3Renderer.UniformBlockBinding_ShadowInfo);
		_bindUBO("VoxelConeData",OGL3Renderer.UniformBlockBinding_VoxelInfo);
		//Texture Bindings//
		_bindTextureTarget("tDiffuse", OGL3Renderer.TextureBinding_tDiffuse);
		_bindTextureTarget("tNormal",OGL3Renderer.TextureBinding_tNormal);
		_bindTextureTarget("tPBR",OGL3Renderer.TextureBinding_tPBR);
		_bindTextureTarget("tItemDiffuse",OGL3Renderer.TextureBinding_tItemDiffuse);
		_bindTextureTarget("skyMap",OGL3Renderer.TextureBinding_SkyCubeMap);
		//
		_bindTextureTarget("gDiffuse",OGL3Renderer.TextureBinding_GBufferDiffuse);
		_bindTextureTarget("gPBR",OGL3Renderer.TextureBinding_GBufferPBR);
		_bindTextureTarget("gNormal",OGL3Renderer.TextureBinding_GBufferNormal);
		_bindTextureTarget("gLighting",OGL3Renderer.TextureBinding_GBufferLighting);
		_bindTextureTarget("gDepth",OGL3Renderer.TextureBinding_GBufferDepth);
		//
		_bindTextureTarget("tMerged",OGL3Renderer.TextureBinding_MergedTextureTarget);
		//
		_bindTextureTarget("shadows",OGL3Renderer.TextureBinding_Shadows);
		//
		_bindTextureTarget("nearVoxel",OGL3Renderer.TextureBinding_NearVoxelMap);
		glUseProgram(0);
	}

	private void _bindUBO(String Name,int uniformBinding) {
		int index = glGetUniformBlockIndex(program_ID, Name);
		if (index != GL_INVALID_INDEX) {
			glUniformBlockBinding(program_ID,index,uniformBinding);
		}else {
			Logger.getLogger("Shader UBO Binder : " + DEBUG)
					.Debug("Failed to get Index of Uniform Buffer Object -> " + Name);
		}
	}

	private void _bindTextureTarget(String Name, int textureTarget) {
		int loc = glGetUniformLocation(program_ID,Name);
		if(loc != GL_INVALID_INDEX) {
			glUniform1i(loc,textureTarget);
		}else{
			Logger.getLogger("Shader Sampler Uniform : " + DEBUG)
					.Debug("Failed to get Sampler Location -> " + Name);
		}
	}

	private String _getPre() {
		if(shaderPreResource.checkIfChanged()) {
			shaderPreResource.reloadData();
		}
		return shaderPreResource.getStringData();
	}

	@Override
	protected void _storeShaderSource(int id, String source) {
		//Append Information to Source//
		String newSource = source.replace("#include worldRender",_getPre());
		super._storeShaderSource(id, newSource);
	}
}
