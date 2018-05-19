package net.openvoxel.client.renderer.vk.pipeline.impl;

import net.openvoxel.client.renderer.vk.pipeline.VulkanGraphicsPipeline;
import net.openvoxel.client.renderer.vk.pipeline.VulkanShaderModule;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanPostProcessPipeline extends VulkanGraphicsPipeline {

	public VulkanPostProcessPipeline(VulkanShaderModule module) {
		super(module);
	}

	@Override
	protected VkPipelineVertexInputStateCreateInfo getVertexState(MemoryStack stack) {
		VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
		vertexInput.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		vertexInput.pNext(VK_NULL_HANDLE);
		vertexInput.flags(0);
		vertexInput.pVertexBindingDescriptions(null);
		vertexInput.pVertexAttributeDescriptions(null);
		return vertexInput;
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
			attachments.blendEnable(false);
			attachments.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
			attachments.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
			attachments.colorBlendOp(VK_BLEND_OP_ADD);
			attachments.srcAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
			attachments.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
			attachments.alphaBlendOp(VK_BLEND_OP_ADD);
			attachments.colorWriteMask(
					VK_COLOR_COMPONENT_R_BIT |
					VK_COLOR_COMPONENT_G_BIT |
					VK_COLOR_COMPONENT_B_BIT |
					VK_COLOR_COMPONENT_A_BIT
			);
		}
		attachments.position(0);
		blendState.pAttachments(attachments);
		blendState.blendConstants(0,0.0F);
		blendState.blendConstants(1,0.0F);
		blendState.blendConstants(2,0.0F);
		blendState.blendConstants(3,0.0F);
		return blendState;
	}
}
