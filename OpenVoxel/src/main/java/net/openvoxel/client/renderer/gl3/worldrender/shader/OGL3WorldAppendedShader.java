package net.openvoxel.client.renderer.gl3.worldrender.shader;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.gl3.util.OGL3BasicShader;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;
import org.lwjgl.opengl.GL31;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL31.*;

/**
 * Created by James on 11/09/2016.
 *
 * World Shader w/ append
 */
public class OGL3WorldAppendedShader extends OGL3BasicShader{

	static final ResourceHandle shaderPreResource;
	static {
		shaderPreResource = ResourceManager.getResource(ResourceType.SHADER,"world/worldShaderPre");
	}

	public OGL3WorldAppendedShader(String shaderSource, String debugID) {
		super(shaderSource, debugID);
		_bindUBO(OGL3World_UniformCache.UBO_Settings,"SETTINGS");
		_bindUBO(OGL3World_UniformCache.UBO_ChunkConstants,"ChunkConstants");
		_bindUBO(OGL3World_UniformCache.UBO_FinalFrame,"FinalFrame");
		_bindUBO(OGL3World_UniformCache.UBO_TextureAtlas,"TextureAtlas");
		_bindUBO(OGL3World_UniformCache.UBO_ShadowMap,"ShadowMap");
	}

	private void _bindUBO(int ubo,String Name) {
		glBindBuffer(GL_UNIFORM_BUFFER, ubo);
		int index = glGetUniformBlockIndex(program_ID, Name);
		if (index != GL_INVALID_INDEX) {
			GL31.glUniformBlockBinding(program_ID, 0, index);
		}else {
			Logger.getLogger("Shader UBO Binder : " + DEBUG).Warning("Failed to get Index of Uniform Buffer Object");
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
