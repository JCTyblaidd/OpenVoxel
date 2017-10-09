package net.openvoxel.client.renderer.vk.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VkShaderPipelineDebug extends VkShaderPipelineBase{

	public VkShaderPipelineDebug(VkShaderModuleCache cache) {
		super(cache);
	}

	@Override
	VkPipelineVertexInputStateCreateInfo genInputState(MemoryStack stack) {
		VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
		vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		vertexInputInfo.pNext(VK_NULL_HANDLE);
		vertexInputInfo.flags(0);
		vertexInputInfo.pVertexBindingDescriptions(null);
		vertexInputInfo.pVertexAttributeDescriptions(null);
		return vertexInputInfo;
	}

	@Override
	VkPipelineRasterizationStateCreateInfo genRasterState(MemoryStack stack) {
		VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack);
		rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterizer.pNext(VK_NULL_HANDLE);
		rasterizer.flags(0);
		rasterizer.depthClampEnable(false);
		rasterizer.rasterizerDiscardEnable(false);
		rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
		rasterizer.lineWidth(1.0f);
		rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);
		rasterizer.depthBiasEnable(false);
		return rasterizer;
	}

	@Override
	VkPipelineLayoutCreateInfo genPipelineLayout(MemoryStack stack) {
		VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.mallocStack(stack);
		layoutCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
		layoutCreateInfo.pNext(VK_NULL_HANDLE);
		layoutCreateInfo.flags(0);
		layoutCreateInfo.pPushConstantRanges(null);
		layoutCreateInfo.pSetLayouts(null);
		return layoutCreateInfo;
	}
}