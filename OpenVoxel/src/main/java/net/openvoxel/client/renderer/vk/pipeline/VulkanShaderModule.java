package net.openvoxel.client.renderer.vk.pipeline;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShaderModule {

	private ResourceHandle handle;
	private String uniqueID;
	private boolean has_glsl_extensions;

	private static int MODULE_VERTEX = 0;
	private static int MODULE_FRAGMENT = 1;
	private static int MODULE_TESS_EVAL = 2;
	private static int MODULE_TESS_CONTROL = 3;
	private static int MODULE_GEOMETRY = 4;
	private static int MODULE_COUNT = 5;

	private ByteBuffer[] SpirV = new ByteBuffer[MODULE_COUNT];
	private long[] ShaderModules = new long[MODULE_COUNT];
	private static int[] ShaderBits = new int[]{
		VK_SHADER_STAGE_VERTEX_BIT,
		VK_SHADER_STAGE_FRAGMENT_BIT,
		VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT,
		VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT,
		VK_SHADER_STAGE_GEOMETRY_BIT
	};
	private static String[] Pragmas = new String[]{
			"vertex",
			"fragment",
			"tesseval",
			"tesscontrol",
			"geometry"
	};

	public VulkanShaderModule(String id, ResourceHandle handle) {
		this.uniqueID = id;
		this.handle = handle;
	}

	public void loadModules(VulkanDevice device, List<String> enabledDefines) {
		handle.reloadData();
		has_glsl_extensions = device.enabled_NV_glsl_shader;
		parseSource(handle.getStringData(),enabledDefines);
		try(MemoryStack stack = stackPush()) {
			LongBuffer pModule = stack.mallocLong(1);
			VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			for(int i = 0; i < MODULE_COUNT; i++) {
				if(SpirV[i] != null) {
					createInfo.pCode(SpirV[i]);
					int res = vkCreateShaderModule(device.logicalDevice, createInfo, null, pModule);
					if(res == VK_SUCCESS) {
						ShaderModules[i] = pModule.get(0);
						VulkanUtility.LogInfo("Successfully Compiled Module: " + uniqueID+"("+Pragmas[i]+")");
					}else if(res == VK_ERROR_INVALID_SHADER_NV) {
						VulkanUtility.LogWarn("NV_GLSL_Shader: Invalid Shader");
						VulkanUtility.CrashOnBadResult("Failed to compile Shader",res);
					}else {
						VulkanUtility.LogWarn("Failed to compile Shader: No Memory");
						VulkanUtility.CrashOnBadResult("Failed to compile Shader",res);
					}
				}else{
					ShaderModules[i] = VK_NULL_HANDLE;
				}
			}
		}
	}

	TIntList getShaderTypes() {
		TIntList ret = new TIntArrayList();
		for(int i = 0; i < MODULE_COUNT; i++) {
			if(ShaderModules[i] != VK_NULL_HANDLE) {
				ret.add(ShaderBits[i]);
			}
		}
		return ret;
	}

	long getShaderModuleFromBit(int shaderBit) {
		for(int i = 0; i < MODULE_COUNT; i++) {
			if(ShaderBits[i] == shaderBit) {
				return ShaderModules[i];
			}
		}
		return VK_NULL_HANDLE;
	}

	public void unloadModules(VkDevice device) {
		handle.unloadData();
		for(int i = 0; i < MODULE_COUNT; i++) {
			vkDestroyShaderModule(device,ShaderModules[i],null);
			ShaderModules[i] = VK_NULL_HANDLE;
			////
			MemoryUtil.memFree(SpirV[i]);
			SpirV[i] = null;
		}
	}


	private void storeSources(int shaderType,int oldIdx,int newIdx,String[] lines,List<String> shaderDefines) {
		if (shaderType == -1) return;
		StringBuilder builder = new StringBuilder();
		builder.append("#version 450\n");
		builder.append("#pragma shader_stage(");
		builder.append(Pragmas[shaderType]);
		builder.append(")\n");
		builder.append("#extension GL_ARB_separate_shader_objects : enable\n");
		for (int i = oldIdx + 1; i < newIdx; i++) {
			builder.append(lines[i]);
			builder.append('\n');
		}
		ByteBuffer compiledShader = VulkanSpirVCompiler.compileShader(has_glsl_extensions,builder.toString(),shaderDefines);
		if(compiledShader == null) {
			VulkanUtility.getSubLogger("Shader Compiler");
			CrashReport crashReport = new CrashReport("Failed to compile shader");
			OpenVoxel.reportCrash(crashReport);
		}
		SpirV[shaderType] = compiledShader;
	}

	private void parseSource(String shaderSource,List<String> shaderDefines) {
		String[] lines = shaderSource.split("\n");
		int last_index = 0;
		int last_loading = -1;
		for(int i = 0; i < lines.length; i++) {
			if(lines[i].startsWith("/**[VK::Vertex]**/")) {
				storeSources(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = MODULE_VERTEX;
			}else if(lines[i].startsWith("/**[VK::Fragment]**/")) {
				storeSources(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = MODULE_FRAGMENT;
			}else if(lines[i].startsWith("/**[VK::Tess-Eval]**/")) {
				storeSources(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = MODULE_TESS_EVAL;
			}else if(lines[i].startsWith("/**[VK::Tess-Control]**/")) {
				storeSources(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = MODULE_TESS_CONTROL;
			}else if(lines[i].startsWith("/**[VK::Geometry]**/")) {
				storeSources(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = MODULE_GEOMETRY;
			}else if(lines[i].startsWith("/**[VK::End]**/")) {
				storeSources(last_loading,last_index,i,lines,shaderDefines);
				last_index = i;
				last_loading = -1;
			}
		}
	}

}
