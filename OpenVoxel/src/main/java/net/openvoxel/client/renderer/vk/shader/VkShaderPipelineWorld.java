package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

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
	VkPipelineDepthStencilStateCreateInfo genDepthStencilState(MemoryStack stack) {
		VkStencilOpState stencilOp = VkStencilOpState.mallocStack(stack);
		stencilOp.set(0,0,0,0,0,0,0);
		VkPipelineDepthStencilStateCreateInfo depthState = VkPipelineDepthStencilStateCreateInfo.mallocStack(stack);
		depthState.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
		depthState.pNext(VK_NULL_HANDLE);
		depthState.flags(0);
		depthState.depthTestEnable(true);
		depthState.depthWriteEnable(true);
		depthState.depthCompareOp(VK_COMPARE_OP_LESS);
		depthState.depthBoundsTestEnable(true);
		depthState.stencilTestEnable(false);
		depthState.front(stencilOp);
		depthState.back(stencilOp);
		depthState.minDepthBounds(0.0F);
		depthState.maxDepthBounds(0.1F);
		return depthState;
	}

	@Override
	VkPipelineRasterizationStateCreateInfo genRasterState(MemoryStack stack) {
		VkPipelineRasterizationStateCreateInfo rasterState = VkPipelineRasterizationStateCreateInfo.mallocStack(stack);
		rasterState.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterState.pNext(VK_NULL_HANDLE);
		rasterState.flags(0);
		rasterState.depthClampEnable(true);
		rasterState.rasterizerDiscardEnable(true);
		rasterState.polygonMode(VK_POLYGON_MODE_FILL);
		rasterState.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterState.frontFace(VK_FRONT_FACE_CLOCKWISE);
		rasterState.depthBiasEnable(false);
		rasterState.depthBiasConstantFactor(0.0f);
		rasterState.depthBiasClamp(0.0f);
		rasterState.depthBiasSlopeFactor(0.0f);
		rasterState.lineWidth(1.0F);
		return rasterState;
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
