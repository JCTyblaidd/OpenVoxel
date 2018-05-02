package net.openvoxel.client.renderer.vk.pipeline;

import gnu.trove.list.TIntList;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV;
import static org.lwjgl.vulkan.VK10.*;

public abstract class VulkanGraphicsPipeline {

	private VulkanShaderModule shaders;
	private long graphicsPipeline;

	public long getPipeline() {
		return graphicsPipeline;
	}

	public VulkanGraphicsPipeline(VulkanShaderModule module) {
		this.shaders = module;
		graphicsPipeline = VK_NULL_HANDLE;
	}

	public void generate(VkDevice device,long layout,long renderPass,int subPass,long pipelineCache,long basePipeline) {
		try(MemoryStack stack = stackPush()) {
			VkGraphicsPipelineCreateInfo.Buffer createInfo = VkGraphicsPipelineCreateInfo.mallocStack(1,stack);
			createInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(getFlags());
			createInfo.pStages(getShaderStages(stack));
			createInfo.pVertexInputState(getVertexState(stack));
			createInfo.pInputAssemblyState(getInputState(stack));
			createInfo.pTessellationState(getTessellationState(stack));
			createInfo.pViewportState(getViewportState(stack));
			createInfo.pRasterizationState(getRasterizationState(stack));
			createInfo.pMultisampleState(getMultisampleState(stack));
			createInfo.pDepthStencilState(getDepthStencilState(stack));
			createInfo.pColorBlendState(getColorBlendState(stack));
			createInfo.pDynamicState(getDynamicState(stack));
			createInfo.layout(layout);
			createInfo.renderPass(renderPass);
			createInfo.subpass(subPass);
			createInfo.basePipelineHandle(basePipeline);
			createInfo.basePipelineIndex(-1);
			//Create Pipeline
			LongBuffer pPipelines = stack.mallocLong(1);
			int vkResult = vkCreateGraphicsPipelines(device,pipelineCache,createInfo,null,pPipelines);
			if(vkResult == VK_SUCCESS) {
				graphicsPipeline = pPipelines.get(0);
			}else if(vkResult == VK_ERROR_INVALID_SHADER_NV) {
				VulkanUtility.LogSevere("Failed to create graphics pipeline: invalid shader");
				VulkanUtility.CrashOnBadResult("Failed to create graphics pipeline",vkResult);
			}else{
				VulkanUtility.LogWarn("Failed to create graphics pipeline: out of memory");
				//TODO: Out of Memory
				VulkanUtility.CrashOnBadResult("Failed to create graphics pipeline",vkResult);
			}
		}
	}

	protected int getFlags() {
		return 0;
	}

	protected VkSpecializationInfo getShaderSpecialization(MemoryStack stack) {
		return null;
	}

	protected VkPipelineShaderStageCreateInfo.Buffer getShaderStages(MemoryStack stack) {
		TIntList stageList = shaders.getShaderTypes();
		VkPipelineShaderStageCreateInfo.Buffer createInfos = VkPipelineShaderStageCreateInfo.mallocStack(stageList.size(),stack);
		ByteBuffer entryName = stack.UTF8("main");
		VkSpecializationInfo specializationInfo = getShaderSpecialization(stack);
		for(int i = 0; i < stageList.size(); i++) {
			createInfos.position(i);
			createInfos.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
			createInfos.pNext(VK_NULL_HANDLE);
			createInfos.flags(0);
			createInfos.stage(stageList.get(i));
			createInfos.module(shaders.getShaderModuleFromBit(stageList.get(i)));
			createInfos.pName(entryName);
			createInfos.pSpecializationInfo(specializationInfo);
		}
		createInfos.position(0);
		return createInfos;
	}

	protected abstract VkPipelineVertexInputStateCreateInfo getVertexState(MemoryStack stack);

	protected VkPipelineInputAssemblyStateCreateInfo getInputState(MemoryStack stack) {
		VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.mallocStack(stack);
		inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
		inputAssembly.pNext(VK_NULL_HANDLE);
		inputAssembly.flags(0);
		inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
		inputAssembly.primitiveRestartEnable(false);
		return inputAssembly;
	}

	protected VkPipelineTessellationStateCreateInfo getTessellationState(MemoryStack stack) {
		VkPipelineTessellationStateCreateInfo tessellateState = VkPipelineTessellationStateCreateInfo.mallocStack(stack);
		tessellateState.sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO);
		tessellateState.pNext(VK_NULL_HANDLE);
		tessellateState.flags(0);
		//TODO: give method of specifying this via the shader
		tessellateState.patchControlPoints(8);
		return tessellateState;
	}

	protected VkPipelineViewportStateCreateInfo getViewportState(MemoryStack stack) {
		VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.mallocStack(stack);
		viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
		viewportState.pNext(VK_NULL_HANDLE);
		viewportState.flags(0);
		//Are Both Dynamic State : But Are Required!!!!
		VkViewport.Buffer fallbackViewport = VkViewport.mallocStack(1,stack);
		fallbackViewport.get(0).set(0,0,
				100,100,0,1);

		VkRect2D.Buffer fallbackScissor = VkRect2D.callocStack(1,stack);
		fallbackScissor.extent().set(100,100);
		fallbackScissor.offset().set(0,0);

		viewportState.pViewports(fallbackViewport);
		viewportState.pScissors(fallbackScissor);
		viewportState.viewportCount(1);
		viewportState.scissorCount(1);
		return viewportState;
	}

	protected VkPipelineRasterizationStateCreateInfo getRasterizationState(MemoryStack stack) {
		VkPipelineRasterizationStateCreateInfo rasterState = VkPipelineRasterizationStateCreateInfo.mallocStack(stack);
		rasterState.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterState.pNext(VK_NULL_HANDLE);
		rasterState.flags(0);
		rasterState.depthClampEnable(false);       //was false -> true??
		rasterState.rasterizerDiscardEnable(false);//was true -> false???
		rasterState.polygonMode(VK_POLYGON_MODE_FILL);
		rasterState.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterState.frontFace(VK_FRONT_FACE_CLOCKWISE);
		rasterState.depthBiasEnable(false);
		rasterState.depthBiasConstantFactor(0.0F);
		rasterState.depthBiasClamp(0.0F);
		rasterState.depthBiasSlopeFactor(0.0F);
		rasterState.lineWidth(1.0F);
		return rasterState;
	}

	protected VkPipelineMultisampleStateCreateInfo getMultisampleState(MemoryStack stack) {
		VkPipelineMultisampleStateCreateInfo multiSampleState = VkPipelineMultisampleStateCreateInfo.mallocStack(stack);
		multiSampleState.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
		multiSampleState.pNext(VK_NULL_HANDLE);
		multiSampleState.flags(0);
		multiSampleState.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
		multiSampleState.sampleShadingEnable(false);
		multiSampleState.minSampleShading(0.0F);
		multiSampleState.pSampleMask(null);
		multiSampleState.alphaToCoverageEnable(false);
		multiSampleState.alphaToOneEnable(false);
		return multiSampleState;
	}

	protected VkPipelineDepthStencilStateCreateInfo getDepthStencilState(MemoryStack stack) {
		VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.mallocStack(stack);
		depthStencilState.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
		depthStencilState.pNext(VK_NULL_HANDLE);
		depthStencilState.flags(0);
		depthStencilState.depthTestEnable(true);
		depthStencilState.depthWriteEnable(true);
		depthStencilState.depthCompareOp(VK_COMPARE_OP_LESS);
		depthStencilState.depthBoundsTestEnable(true);
		depthStencilState.stencilTestEnable(false);
		//depthStencilState.front()
		//depthStencilState.front();
		//depthStencilState.back();
		depthStencilState.minDepthBounds(0.0F);
		depthStencilState.maxDepthBounds(1.0F);
		return depthStencilState;
	}

	protected abstract VkPipelineColorBlendStateCreateInfo getColorBlendState(MemoryStack stack);

	protected VkPipelineDynamicStateCreateInfo getDynamicState(MemoryStack stack) {
		VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.mallocStack(stack);
		dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
		dynamicState.pNext(VK_NULL_HANDLE);
		dynamicState.flags(0);
		dynamicState.pDynamicStates(
				stack.ints(
						VK_DYNAMIC_STATE_VIEWPORT,
						VK_DYNAMIC_STATE_SCISSOR
				)
		);
		return dynamicState;
	}


	///


	public void free(VkDevice device) {
		if(graphicsPipeline != VK_NULL_HANDLE) {
			vkDestroyPipeline(device, graphicsPipeline, null);
			graphicsPipeline = VK_NULL_HANDLE;
		}
	}



}
