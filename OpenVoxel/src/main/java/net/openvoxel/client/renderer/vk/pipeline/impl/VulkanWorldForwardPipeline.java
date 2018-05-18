package net.openvoxel.client.renderer.vk.pipeline.impl;

import net.openvoxel.client.renderer.vk.pipeline.VulkanGraphicsPipeline;
import net.openvoxel.client.renderer.vk.pipeline.VulkanShaderModule;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;

/*
 * Inputs:
 *  vec3 (float) x,y,z
 *  vec2 (short) u,v
 *  vec3 (byte)  norm
 *  vec3 (byte)  tang
 *  vec3 (byte)  col
 *  vec4 (byte)  light
 *  vec2 (float)  animation
 */
public class VulkanWorldForwardPipeline extends VulkanGraphicsPipeline {

	public VulkanWorldForwardPipeline(VulkanShaderModule module) {
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
			inputBinding.stride(32);
			inputBinding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
		}
		inputBinding.position(0);
		vertexInputState.pVertexBindingDescriptions(inputBinding);

		VkVertexInputAttributeDescription.Buffer inputAttributes = VkVertexInputAttributeDescription.mallocStack(7,stack);
		inputAttributes.position(0);
		{
			inputAttributes.location(0);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R32G32B32_SFLOAT);
			inputAttributes.offset(0);
		}
		inputAttributes.position(1);
		{
			inputAttributes.location(1);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R16G16_UNORM);
			inputAttributes.offset(12);
		}
		inputAttributes.position(2);
		{
			inputAttributes.location(2);//TODO: CHANGE SHODDY RENDER_DOC FIX BACK AFTERWARDS
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R8G8B8A8_UNORM);//VK_FORMAT_R8G8B8_UNORM);
			inputAttributes.offset(16);
		}
		inputAttributes.position(3);
		{
			inputAttributes.location(3);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R8G8_UNORM);//VK_FORMAT_R8G8B8_UNORM);
			inputAttributes.offset(20);//19);
		}
		inputAttributes.position(4);
		{
			inputAttributes.location(4);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R8G8B8A8_UNORM);
			inputAttributes.offset(22);
		}
		inputAttributes.position(5);
		{
			inputAttributes.location(5);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R8G8B8A8_UNORM);
			inputAttributes.offset(26);
		}
		inputAttributes.position(6);
		{
			inputAttributes.location(6);
			inputAttributes.binding(0);
			inputAttributes.format(VK_FORMAT_R8G8_UNORM);
			inputAttributes.offset(30);
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

	@Override
	protected VkPipelineRasterizationStateCreateInfo getRasterizationState(MemoryStack stack) {
		VkPipelineRasterizationStateCreateInfo rasterState = VkPipelineRasterizationStateCreateInfo.mallocStack(stack);
		rasterState.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterState.pNext(VK_NULL_HANDLE);
		rasterState.flags(0);
		rasterState.depthClampEnable(false);       //was false -> true??
		rasterState.rasterizerDiscardEnable(false);//was true -> false???
		rasterState.polygonMode(VK_POLYGON_MODE_FILL);
		rasterState.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterState.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
		rasterState.depthBiasEnable(false);
		rasterState.depthBiasConstantFactor(0.0F);
		rasterState.depthBiasClamp(0.0F);
		rasterState.depthBiasSlopeFactor(0.0F);
		rasterState.lineWidth(1.0F);
		return rasterState;
	}


}
