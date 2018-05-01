package net.openvoxel.client.renderer.vk.pipeline.impl;

import net.openvoxel.client.renderer.vk.pipeline.VulkanGraphicsPipeline;
import net.openvoxel.client.renderer.vk.pipeline.VulkanShaderModule;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 *
 * Input: Interleaved[
 *                      vec2(signed normal short) pos;
 *                      vec2(signed normal short) uv;
 *                      vec4(signed normal byte) rgba;
 *                  ]
 *
 */
public class VulkanGuiPipeline extends VulkanGraphicsPipeline {

	public VulkanGuiPipeline(VulkanShaderModule module) {
		super(module);
	}

	@Override
	protected VkPipelineVertexInputStateCreateInfo getVertexState(MemoryStack stack) {
		VkPipelineVertexInputStateCreateInfo vertexInputState = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
		vertexInputState.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		vertexInputState.pNext(VK_NULL_HANDLE);
		vertexInputState.flags(0);

		VkVertexInputBindingDescription.Buffer inputBinding = VkVertexInputBindingDescription.mallocStack(1,stack);
		inputBinding.position(0);
		{
			inputBinding.binding(0);
			inputBinding.stride(4 + 4 + 4 + 4 + 4);
			inputBinding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
		}
		inputBinding.position(0);
		vertexInputState.pVertexBindingDescriptions(inputBinding);

		VkVertexInputAttributeDescription.Buffer inputAttributes = VkVertexInputAttributeDescription.mallocStack(3,stack);
		inputAttributes.position(0);
		{
			inputAttributes.location(0);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R32G32_SFLOAT);
			inputAttributes.offset(0);
		}
		inputAttributes.position(1);
		{
			inputAttributes.location(1);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R32G32_SFLOAT);
			inputAttributes.offset(4+4);
		}
		inputAttributes.position(2);
		{
			inputAttributes.location(2);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R8G8B8A8_SNORM);
			inputAttributes.offset(4+4+4+4);
		}
		inputAttributes.position(0);
		vertexInputState.pVertexAttributeDescriptions(inputAttributes);
		return vertexInputState;
	}



	@Override
	protected VkPipelineColorBlendStateCreateInfo getColorBlendState(MemoryStack stack) {
		VkPipelineColorBlendStateCreateInfo blendState = VkPipelineColorBlendStateCreateInfo.mallocStack(stack);
		blendState.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
		blendState.pNext(VK_NULL_HANDLE);
		blendState.flags(0);
		blendState.logicOpEnable(false);
		blendState.logicOp(0);
		VkPipelineColorBlendAttachmentState.Buffer attachments = VkPipelineColorBlendAttachmentState.mallocStack(1,stack);
		attachments.position(0);
		{
			attachments.blendEnable(true);
			attachments.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
			attachments.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
			attachments.colorBlendOp(VK_BLEND_OP_ADD);
			attachments.srcAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
			attachments.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
			attachments.alphaBlendOp(VK_BLEND_OP_ADD);
			attachments.colorWriteMask(0xFFFFFFFF);
		}
		attachments.position(0);
		blendState.pAttachments(attachments);
		blendState.blendConstants(0,0.0F);
		blendState.blendConstants(1,0.0F);
		blendState.blendConstants(2,0.0F);
		blendState.blendConstants(3,0.0F);
		return blendState;
	}

	/*
	 * Depth & Stencil Testing All disabled...
	 */
	@Override
	protected VkPipelineDepthStencilStateCreateInfo getDepthStencilState(MemoryStack stack) {
		VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.mallocStack(stack);
		depthStencilState.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
		depthStencilState.pNext(VK_NULL_HANDLE);
		depthStencilState.flags(0);
		depthStencilState.depthTestEnable(false);
		depthStencilState.depthWriteEnable(false);
		depthStencilState.depthCompareOp(VK_COMPARE_OP_LESS);
		depthStencilState.depthBoundsTestEnable(false);
		depthStencilState.stencilTestEnable(false);
		//depthStencilState.front();
		//depthStencilState.back();
		depthStencilState.minDepthBounds(0.0F);
		depthStencilState.maxDepthBounds(1.0F);
		return depthStencilState;
	}

}