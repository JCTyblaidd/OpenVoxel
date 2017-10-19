package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkShaderPipelineGUI extends VkShaderPipelineBase {

	public long DescriptorSetLayout;

	public VkShaderPipelineGUI(VkShaderModuleCache cache) {
		super(cache);
	}

	@Override
	public void destroy(VkDevice device) {
		super.destroy(device);
		vkDestroyDescriptorSetLayout(device,DescriptorSetLayout,null);
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
	VkPipelineLayoutCreateInfo genPipelineLayout(VkDevice device,MemoryStack stack) {
		VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.mallocStack(1,stack);
		pushConstants.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
		pushConstants.offset(0);
		pushConstants.size(2 * Integer.SIZE / Byte.SIZE);

		VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.mallocStack(1,stack);
		layoutBindings.binding(0);
		layoutBindings.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
		layoutBindings.descriptorCount(32);
		layoutBindings.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
		layoutBindings.pImmutableSamplers(null);

		VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.mallocStack(stack);
		layoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
		layoutCreateInfo.pNext(VK_NULL_HANDLE);
		layoutCreateInfo.flags(0);
		layoutCreateInfo.pBindings(layoutBindings);

		LongBuffer setLayouts = stack.callocLong(1);

		if(vkCreateDescriptorSetLayout(device,layoutCreateInfo,null,setLayouts) != VK_SUCCESS) {
			throw new RuntimeException("Failed to create descriptor set layout");
		}

		DescriptorSetLayout = setLayouts.get(0);

		VkPipelineLayoutCreateInfo PipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.mallocStack(stack);
		PipelineLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
		PipelineLayoutCreateInfo.pNext(VK_NULL_HANDLE);
		PipelineLayoutCreateInfo.flags(0);
		PipelineLayoutCreateInfo.pSetLayouts(setLayouts);
		PipelineLayoutCreateInfo.pPushConstantRanges(pushConstants);
		return PipelineLayoutCreateInfo;
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
