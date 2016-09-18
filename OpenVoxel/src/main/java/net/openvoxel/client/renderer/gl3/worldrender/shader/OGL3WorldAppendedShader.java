package net.openvoxel.client.renderer.gl3.worldrender.shader;

import net.openvoxel.client.renderer.gl3.util.OGL3BasicShader;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;

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
