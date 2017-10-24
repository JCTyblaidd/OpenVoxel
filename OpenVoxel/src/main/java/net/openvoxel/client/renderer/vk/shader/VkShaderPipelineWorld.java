package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;

public class VkShaderPipelineWorld extends VkShaderPipelineBase {

	public VkShaderPipelineWorld(VkShaderModuleCache cache) {
		super(cache);
	}

	@Override
	VkPipelineVertexInputStateCreateInfo genInputState(MemoryStack stack) {
		VkVertexInputBindingDescription.Buffer vertexBinding = VkVertexInputBindingDescription.mallocStack(1,stack);
		vertexBinding.position(0);
		vertexBinding.binding(0);
		vertexBinding.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
		vertexBinding.stride();

		VkVertexInputAttributeDescription.Buffer vertexAttribute = VkVertexInputAttributeDescription.mallocStack(1,stack);
		vertexAttribute.position(0);
		vertexAttribute.location();
		vertexAttribute.binding();
		vertexAttribute.format();
		vertexAttribute.offset();

		VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
		vertexInput.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		vertexInput.pNext(VK_NULL_HANDLE);
		vertexInput.flags(0);
		vertexInput.pVertexBindingDescriptions(vertexBinding);
		vertexInput.pVertexAttributeDescriptions(vertexAttribute);
		return vertexInput;
	}

	@Override
	VkPipelineRasterizationStateCreateInfo genRasterState(MemoryStack stack) {
		return null;
	}

	@Override
	VkPipelineColorBlendStateCreateInfo genColorBlendState(MemoryStack stack) {
		return super.genColorBlendState(stack);
	}

	@Override
	VkPipelineLayoutCreateInfo genPipelineLayout(VkDevice device, MemoryStack stack) {
		return null;
	}

	@Override
	VkPipelineDynamicStateCreateInfo genDynamicState(MemoryStack stack) {
		return super.genDynamicState(stack);
	}
}
