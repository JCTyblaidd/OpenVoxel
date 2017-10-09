package net.openvoxel.client.renderer.vk.shader;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.vk.util.VkShaderCompiler;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VkShaderModuleCache {

	public static VkShaderModuleCache guiShader;
	public static VkShaderModuleCache debugShader;

	static {
		guiShader = new VkShaderModuleCache("gui/guiShader");
		debugShader = new VkShaderModuleCache("debug/debugTriangle");
	}

	///Local Code//

	private ByteBuffer spivVertex = null;
	private ByteBuffer spivFragment = null;
	private ByteBuffer spivTessEval = null;
	private ByteBuffer spivTessCntl = null;
	private ByteBuffer spivGeometry = null;
	private ResourceHandle res;
	private String resName;

	private VkShaderModuleCache(String resourceID) {
		resName = resourceID;
		res = ResourceManager.getResource(ResourceType.SHADER, resourceID);
	}

	public void load(List<String> shaderDefines) {
		res.reloadData();
		String str = res.getStringData();
		_parse(str,shaderDefines);
	}

	private ByteBuffer _free(ByteBuffer buffer) {
		if(buffer != null) {
			MemoryUtil.memFree(buffer);
		}
		return null;
	}

	public void release() {
		spivVertex = _free(spivVertex);
		spivFragment = _free(spivFragment);
		spivTessCntl = _free(spivTessCntl);
		spivTessEval = _free(spivTessEval);
		spivGeometry = _free(spivGeometry);
	}

	ByteBuffer getShader(int type) {
		switch(type) {
			case VK_SHADER_STAGE_FRAGMENT_BIT:
				return spivFragment;
			case VK_SHADER_STAGE_VERTEX_BIT:
				return spivVertex;
			case VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT:
				return spivTessCntl;
			case VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT:
				return spivTessEval;
			case VK_SHADER_STAGE_GEOMETRY_BIT:
				return spivGeometry;
			default:
				return null;
		}
	}


	TIntList listTypes() {
		TIntList types = new TIntArrayList();
		if(spivFragment != null) types.add(VK_SHADER_STAGE_FRAGMENT_BIT);
		if(spivVertex != null) types.add(VK_SHADER_STAGE_VERTEX_BIT);
		if(spivTessCntl != null) types.add(VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT);
		if(spivTessEval != null) types.add(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT);
		if(spivGeometry != null) types.add(VK_SHADER_STAGE_GEOMETRY_BIT);
		return types;
	}

	private static final String[] pragmas = new String[]{"fragment","vertex","tesscontrol","tesseval","geometry"};
	private void _store(int last_id,int last_index,int curr_index,String[] lines,List<String> shaderDefines) {
		if (last_id == -1) return;
		StringBuilder builder = new StringBuilder();
		builder.append("#version 450\n");
		builder.append("#pragma shader_stage(");
		builder.append(pragmas[last_id]);
		builder.append(")\n");
		builder.append("#extension GL_ARB_separate_shader_objects : enable\n");
		for (int i = last_index + 1; i < curr_index; i++) {
			builder.append(lines[i]);
			builder.append("\n");
		}
		byte[] _src = VkShaderCompiler.compileSpiv(builder.toString(),shaderDefines);
		if(_src == null) {
			Logger.getLogger("Vulkan").getSubLogger("Shader Gen").Severe("Failed to compile shader: " + resName + ":" + pragmas[last_id]);
			throw new RuntimeException("Error With Shader Compilation");
		}
		ByteBuffer data = MemoryUtil.memAlloc(_src.length);
		data.put(_src);
		data.position(0);
		switch(last_id) {
			case 0:
				spivFragment = data;
				break;
			case 1:
				spivVertex = data;
				break;
			case 2:
				spivTessCntl = data;
				break;
			case 3:
				spivTessEval = data;
				break;
			case 4:
				spivGeometry = data;
				break;
		}
	}

	private void _parse(String shaderSource,List<String> shaderDefines) {
		String[] lines = shaderSource.split("\n");
		int last_index = 0;
		int last_loading = -1;
		for(int i = 0; i < lines.length; i++) {
			if(lines[i].startsWith("/**[VK::Fragment]**/")) {
				_store(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = 0;
			}else if(lines[i].startsWith("/**[VK::Vertex]**/")) {
				_store(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = 1;
			}else if(lines[i].startsWith("/**[VK::Tess-Control]**/")) {
				_store(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = 2;
			}else if(lines[i].startsWith("/**[VK::Tess-Eval]**/")) {
				_store(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = 3;
			}else if(lines[i].startsWith("/**[VK::Geometry]**/")) {
				_store(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = 4;
			}else if(lines[i].startsWith("/**[VK::End]**/")) {
				_store(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = -1;
			}
		}
	}
}
