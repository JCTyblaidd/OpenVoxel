package net.openvoxel.client.renderer.vk.shader;

import gnu.trove.list.TIntList;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class VkShaderPipelineBase {

	private VkShaderModuleCache cache;

	public long graphics_pipeline;
	public long graphics_pipeline_layout;
	public LongBuffer shader_modules = null;

	public VkShaderPipelineBase(VkShaderModuleCache cache) {
		this.cache = cache;
	}

	public void init(VkDevice device, long render_pass, int subpass, long pipeline_cache,List<String> shaderDefines) {
		try(MemoryStack stack = stackPush()) {
			VkPipelineLayoutCreateInfo layoutCreateInfo = genPipelineLayout(device,stack);
			LongBuffer lb = stack.callocLong(1);
			if(vkCreatePipelineLayout(device,layoutCreateInfo,null,lb) != VK_SUCCESS) {
				throw new RuntimeException("Error Creating Pipeline Layout");
			}
			graphics_pipeline_layout = lb.get(0);

			cache.load(shaderDefines);
			TIntList cacheTypes = cache.listTypes();
			boolean hasTess = cacheTypes.contains(VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT) || cacheTypes.contains(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT);

			VkGraphicsPipelineCreateInfo.Buffer createInfo = VkGraphicsPipelineCreateInfo.mallocStack(1,stack);
			createInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pStages(genShaderStages(device,stack,cacheTypes));
			createInfo.pVertexInputState(genInputState(stack));
			createInfo.pInputAssemblyState(genInputAssembly(stack));
			createInfo.pViewportState(genViewportState(stack));
			createInfo.pRasterizationState(genRasterState(stack));
			createInfo.pMultisampleState(genMultiSampleState(stack));
			createInfo.pDepthStencilState(genDepthStencilState(stack));
			createInfo.pColorBlendState(genColorBlendState(stack));
			createInfo.pTessellationState(genTessState(stack,hasTess));
			createInfo.pDynamicState(genDynamicState(stack));
			createInfo.layout(graphics_pipeline_layout);
			createInfo.renderPass(render_pass);
			createInfo.subpass(subpass);
			createInfo.basePipelineHandle(VK_NULL_HANDLE);
			createInfo.basePipelineIndex(0);
			if(vkCreateGraphicsPipelines(device,pipeline_cache,createInfo,null,lb) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create shader pipeline");
			}
			graphics_pipeline = lb.get(0);
			//Cleanup Shader Modules//
			for(int i = 0; i < shader_modules.capacity(); i++) {
				vkDestroyShaderModule(device,shader_modules.get(i),null);
			}
			MemoryUtil.memFree(shader_modules);
		}
	}

	public void destroy(VkDevice device) {
		vkDestroyPipelineLayout(device,graphics_pipeline_layout,null);
		vkDestroyPipeline(device,graphics_pipeline,null);
	}

	public void reload(VkDevice device,long render_pass, int subpass, long pipeline_cache,List<String> shaderDefines) {
		destroy(device);
		init(device,render_pass,subpass,pipeline_cache,shaderDefines);
	}

	private VkSpecializationInfo getSpecializeInfo(MemoryStack stack) {
		//VkSpecializationInfo specInfo = VkSpecializationInfo.mallocStack(stack);
		return null;
	}

	private VkPipelineShaderStageCreateInfo.Buffer genShaderStages(VkDevice device, MemoryStack stack, TIntList shaderTypes) {
		int count = shaderTypes.size();
		shader_modules = MemoryUtil.memAllocLong(count);
		LongBuffer ret = stack.callocLong(1);
		for(int i = 0; i < count; i++) {
			VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pCode(cache.getShader(shaderTypes.get(i)));
			if(vkCreateShaderModule(device,createInfo,null,ret) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create shader module");
			}
			shader_modules.put(i,ret.get(0));
		}
		VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.mallocStack(count,stack);
		for(int i = 0; i < count; i++) {
			shaderStages.position(i);
			shaderStages.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
			shaderStages.pNext(VK_NULL_HANDLE);
			shaderStages.flags(0);
			shaderStages.stage(shaderTypes.get(i));
			shaderStages.module(shader_modules.get(i));
			shaderStages.pName(stack.UTF8("main"));
			shaderStages.pSpecializationInfo(getSpecializeInfo(stack));
		}
		shaderStages.position(0);
		cache.release();
		return shaderStages;
	}

	/**
	 * Default: Abstract
	 */
	abstract VkPipelineVertexInputStateCreateInfo genInputState(MemoryStack stack);

	/**
	 * Default: Triangles
	 */
	private VkPipelineInputAssemblyStateCreateInfo genInputAssembly(MemoryStack stack) {
		VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.mallocStack(stack);
		inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
		inputAssembly.pNext(VK_NULL_HANDLE);
		inputAssembly.flags(0);
		inputAssembly.primitiveRestartEnable(false);
		inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
		return inputAssembly;
	}

	/**
	 * Default: Entire Screen
	 */
	private VkPipelineViewportStateCreateInfo genViewportState(MemoryStack stack) {
		VkPipelineViewportStateCreateInfo createInfo = VkPipelineViewportStateCreateInfo.mallocStack(stack);
		VkViewport.Buffer viewport = VkViewport.mallocStack(1,stack);
		VkRect2D.Buffer scissor = VkRect2D.mallocStack(1,stack);
		int width = ClientInput.currentWindowWidth.get();
		int height = ClientInput.currentWindowHeight.get();
		viewport.x(0.0f);
		viewport.y(0.0f);
		viewport.width(width);
		viewport.height(height);
		viewport.minDepth(0.0F);
		viewport.maxDepth(1.0F);
		VkOffset2D offset = VkOffset2D.mallocStack(stack);
		VkExtent2D extent = VkExtent2D.mallocStack(stack);
		offset.set(0,0);
		extent.set(width,height);
		scissor.offset(offset);
		scissor.extent(extent);
		createInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
		createInfo.pNext(VK_NULL_HANDLE);
		createInfo.flags(0);
		createInfo.pViewports(viewport);
		createInfo.pScissors(scissor);
		return createInfo;
	}

	/**
	 * Default; Abstract
	 */
	abstract VkPipelineRasterizationStateCreateInfo genRasterState(MemoryStack stack);

	/**
	 * Default: Single Sample
	 */
	private VkPipelineMultisampleStateCreateInfo genMultiSampleState(MemoryStack stack) {
		VkPipelineMultisampleStateCreateInfo multiSample = VkPipelineMultisampleStateCreateInfo.callocStack(stack);
		multiSample.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
		multiSample.pNext(VK_NULL_HANDLE);
		multiSample.flags(0);
		multiSample.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
		multiSample.sampleShadingEnable(false);
		multiSample.minSampleShading(0.0F);
		multiSample.pSampleMask(null);
		multiSample.alphaToCoverageEnable(false);
		multiSample.alphaToOneEnable(false);
		return multiSample;
	}

	/**
	 * Default: no depth stencil state
	 */
	VkPipelineDepthStencilStateCreateInfo genDepthStencilState(MemoryStack stack) {
		return null;
	}

	/**
	 * Default: Overwrite & one target
	 */
	VkPipelineColorBlendStateCreateInfo genColorBlendState(MemoryStack stack) {
		VkPipelineColorBlendStateCreateInfo colorBlend =  VkPipelineColorBlendStateCreateInfo.mallocStack(stack);
		VkPipelineColorBlendAttachmentState.Buffer defaultAttach = VkPipelineColorBlendAttachmentState.callocStack(1,stack);
		defaultAttach.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
		defaultAttach.blendEnable(false);
		colorBlend.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
		colorBlend.pNext(VK_NULL_HANDLE);
		colorBlend.flags(0);
		colorBlend.logicOpEnable(false);
		colorBlend.logicOp(VK_LOGIC_OP_COPY);
		colorBlend.pAttachments(defaultAttach);
		colorBlend.blendConstants(0, 0.0f);
		colorBlend.blendConstants(1, 0.0f);
		colorBlend.blendConstants(2, 0.0f);
		colorBlend.blendConstants(3, 0.0f);
		return colorBlend;
	}

	/**
	 * Default: no dynamic state
	 */
	VkPipelineDynamicStateCreateInfo genDynamicState(MemoryStack stack) {
		return null;
	}

	/**
	 * Default: warning TODO: impl completely
	 */
	private VkPipelineTessellationStateCreateInfo genTessState(MemoryStack stack,boolean hasTess) {
		if(hasTess) {
			VkPipelineTessellationStateCreateInfo tessState = VkPipelineTessellationStateCreateInfo.mallocStack(stack);
			tessState.sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO);
			tessState.pNext(VK_NULL_HANDLE);
			tessState.flags(0);
			Logger.getLogger("Vulkan").getSubLogger("Shader Gen").Warning("Patch Control Point Value Not Implemented");
			tessState.patchControlPoints(1);//TODO: gen from shader source??
			throw new RuntimeException("Tess Not Supported");
		}else{
			return null;
		}
	}

	/**
	 * Default: Abstract
	 */
	abstract VkPipelineLayoutCreateInfo genPipelineLayout(VkDevice device,MemoryStack stack);

}
