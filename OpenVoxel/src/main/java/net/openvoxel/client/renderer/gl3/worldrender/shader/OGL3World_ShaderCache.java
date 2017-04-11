package net.openvoxel.client.renderer.gl3.worldrender.shader;

import net.openvoxel.client.renderer.gl3.OGL3Renderer;
import net.openvoxel.client.renderer.gl3.util.OGL3ReloadableShader;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;

/**
 * Created by James on 22/09/2016.
 *
 * OpenGL 3 All Shader Storage
 */
public class OGL3World_ShaderCache {

	//GIANT CACHE OF WORLD SHADER CODE
	public static final WorldShaderCache BLOCK_SIMPLE = _get("world/block/shaderWorld_simple","Simple Block Renderer");
	public static final WorldShaderCache GBUFFER_OPAQUE = _get("world/block/shaderWorld_gBufferOpaque","Deferred Block Opaque");
	public static final WorldShaderCache GBUFFER_MERGE = _get("world/block/shaderWorld_Merge","GBuffer Merging Shader");
	public static final WorldShaderCache WORLD_POSTPROCESS = _get("world/block/shaderWorld_Post","World Post Processing Shader");

	public static void Load() {
		OGL3Renderer.gl3Log.Info("Initial Loading of World Shader Code");
	}

	private static WorldShaderCache _get(String str,String debug) {
		return new WorldShaderCache(ResourceManager.getResource(ResourceType.SHADER,str),debug);
	}

	public static class WorldShaderCache extends OGL3ReloadableShader<OGL3WorldAppendedShader> {
		private final String Debug;
		WorldShaderCache(ResourceHandle res,String id) {
			super(res);
			Debug = id;
		}
		@Override
		public OGL3WorldAppendedShader newShader(String src) {
			return new OGL3WorldAppendedShader(src,"World Shader: " + Debug);
		}
	}
}
