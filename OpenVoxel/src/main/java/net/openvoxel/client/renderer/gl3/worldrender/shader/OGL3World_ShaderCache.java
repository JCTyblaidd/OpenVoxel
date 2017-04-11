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
	public static final WorldShaderCache SKYMAP_BACKGROUND = _get("world/util/shaderWorld_SkyDrop","Sky Background Renderer");
	public static final WorldShaderCache BLOCK_SIMPLE = _get("world/block/shaderWorld_simple","Simple Block Renderer");
	public static final WorldShaderCache GBUFFER_OPAQUE = _get("world/block/shaderWorld_gBufferOpaque","Deferred Block Opaque");

	public static final WorldShaderCache BLOCK_FORWARD = _get("world/block/shaderWorld_forwardStore","Forward Renderer Block");
	public static final WorldShaderCache ENTITY_DEFERRED = _get("world/entity/shaderWorld_entity_deferred","Deferred Entity");
	public static final WorldShaderCache ENTITY_FORWARD = _get("world/entity/shaderWorld_entity_forward","Forward Entity");
	public static final WorldShaderCache POST_FINALIZE = _get("world/post/shaderWorld_final","Finalize Deferred");
	public static final WorldShaderCache POST_PROCESS = _get("world/post/shaderWorld_postprocess","Post Processing");
	public static final WorldShaderCache SHADOW_CASCADE_ADV = _get("world/shadow/shaderWorld_cascademap_Adv","Advanced Shadow Map");
	public static final WorldShaderCache SHADOW_CASCADE_SIMPLE = _get("world/shadow/shaderWorld_cascademap_Simple","Simple Shadow Map");
	public static final WorldShaderCache SHADOW_SIMPLE_MAP = _get("world/shadow/shaderWorld_shadowmap","Simple Single Shadow Map");

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
