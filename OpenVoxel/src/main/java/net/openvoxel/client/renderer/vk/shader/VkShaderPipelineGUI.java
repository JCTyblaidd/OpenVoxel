package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE;

public class VkShaderPipelineGUI extends VkShaderPipelineBase {


	public VkShaderPipelineGUI(VkShaderModuleCache cache) {
		super(cache);
	}

	@Override
	VkPipelineVertexInputStateCreateInfo genInputState(MemoryStack stack) {
		VkVertexInputBindingDescription.Buffer inputBinding = VkVertexInputBindingDescription.mallocStack(1,stack);
		inputBinding.position(0);
		inputBinding.binding(0);
		inputBinding.stride(4 + 4 + 4 + 4 + 4);
		inputBinding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

		VkVertexInputAttributeDescription.Buffer inputAttribs = VkVertexInputAttributeDescription.mallocStack(3,stack);
		inputAttribs.position(0);
		inputAttribs.location(0);
		inputAttribs.binding(0);
		inputAttribs.format(VK_FORMAT_R32G32_SFLOAT);
		inputAttribs.offset(0);

		inputAttribs.position(1);
		inputAttribs.location(1);
		inputAttribs.binding(0);
		inputAttribs.format(VK_FORMAT_R32G32_SFLOAT);
		inputAttribs.offset(4+4);

		inputAttribs.position(2);
		inputAttribs.location(2);
		inputAttribs.binding(0);
		inputAttribs.format(VK_FORMAT_R8G8B8A8_UNORM);
		inputAttribs.offset(4+4+4+4);

		inputAttribs.position(0);

		VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
		vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		vertexInputInfo.pNext(VK_NULL_HANDLE);
		vertexInputInfo.flags(0);
		vertexInputInfo.pVertexBindingDescriptions(inputBinding);
		vertexInputInfo.pVertexAttributeDescriptions(inputAttribs);
		return vertexInputInfo;
	}

	@Override
	VkPipelineRasterizationStateCreateInfo genRasterState(MemoryStack stack) {
		VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.mallocStack(stack);
		rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterizer.pNext(VK_NULL_HANDLE);
		rasterizer.flags(0);
		rasterizer.rasterizerDiscardEnable(false);
		rasterizer.depthClampEnable(true);
		rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
		rasterizer.lineWidth(1.0f);
		rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);
		rasterizer.depthBiasEnable(false);
		rasterizer.depthBiasClamp(0.0f);
		rasterizer.depthBiasConstantFactor(0.0f);
		rasterizer.depthBiasSlopeFactor(0.0f);
		return rasterizer;
	}

	VkPipelineColorBlendStateCreateInfo genColorBlendState(MemoryStack stack) {
		VkPipelineColorBlendStateCreateInfo colorBlend =  VkPipelineColorBlendStateCreateInfo.mallocStack(stack);
		VkPipelineColorBlendAttachmentState.Buffer defaultAttach = VkPipelineColorBlendAttachmentState.callocStack(1,stack);
		defaultAttach.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
		defaultAttach.blendEnable(true);
		defaultAttach.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
		defaultAttach.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
		defaultAttach.colorBlendOp(VK_BLEND_OP_ADD);
		defaultAttach.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
		defaultAttach.dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
		defaultAttach.alphaBlendOp(VK_BLEND_OP_ADD);
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

	@Override
	VkPipelineLayoutCreateInfo genPipelineLayout(MemoryStack stack) {
		VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.mallocStack(stack);
		layoutCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
		layoutCreateInfo.pNext(VK_NULL_HANDLE);
		layoutCreateInfo.flags(0);
		layoutCreateInfo.pSetLayouts(null);
		layoutCreateInfo.pPushConstantRanges(null);
		return layoutCreateInfo;
	}

	@Override
	VkPipelineDynamicStateCreateInfo genDynamicState(MemoryStack stack) {
		VkPipelineDynamicStateCreateInfo dynState = VkPipelineDynamicStateCreateInfo.mallocStack(stack);
		dynState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
		dynState.pNext(VK_NULL_HANDLE);
		dynState.flags(0);
		dynState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_SCISSOR));
		return dynState;
	}
}
