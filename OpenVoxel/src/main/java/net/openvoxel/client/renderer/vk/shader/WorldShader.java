package net.openvoxel.client.renderer.vk.shader;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.common.resources.ResourceHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 17/04/2017.
 *
 * World Input Shader
 *
 * layout(location = 0) in vec4 i_pos;//Position//
 layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
 layout(location = 2) in vec3 i_normal;//Normal of Face//
 layout(location = 3) in vec3 i_tangent;//Tangent of Face//
 layout(location = 4) in vec4 i_col;//Colour Mask//
 layout(location = 5) in vec4 i_light;//Lighting Value//
 */
public abstract class WorldShader extends AbstractVkShaderPipeline{


	public WorldShader(VkDeviceState state, ResourceHandle handle) {
		super(state, handle);
	}


	@Override
	protected VkPipelineDynamicStateCreateInfo genDynamicState(MemoryStack stack) {
		VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.mallocStack(stack);
		dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
		dynamicState.pNext(VK_NULL_HANDLE);
		dynamicState.flags(0);
		dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_BLEND_CONSTANTS));
		return dynamicState;
	}

	@Override
	protected VkPipelineMultisampleStateCreateInfo genMultiSampleState(MemoryStack stack) {
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

	@Override
	protected VkPipelineRasterizationStateCreateInfo genRasterizationState(MemoryStack stack) {
		VkPipelineRasterizationStateCreateInfo rasterState = VkPipelineRasterizationStateCreateInfo.mallocStack(stack);
		rasterState.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
		rasterState.pNext(VK_NULL_HANDLE);
		rasterState.flags(0);
		rasterState.depthClampEnable(true);
		rasterState.rasterizerDiscardEnable(true);
		rasterState.polygonMode(VK_POLYGON_MODE_FILL);
		rasterState.cullMode(VK_CULL_MODE_BACK_BIT);
		rasterState.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
		rasterState.depthBiasEnable(false);
		rasterState.depthBiasConstantFactor(0);
		rasterState.depthBiasClamp(0);
		rasterState.depthBiasSlopeFactor(0);
		rasterState.lineWidth(0);
		return rasterState;
	}

	@Override
	protected VkPipelineVertexInputStateCreateInfo genVertexInput(MemoryStack stack) {
		VkPipelineVertexInputStateCreateInfo inputState = VkPipelineVertexInputStateCreateInfo.mallocStack(stack);
		inputState.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
		inputState.pNext(VK_NULL_HANDLE);
		inputState.flags(0);
		VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.mallocStack(6,stack);
		for(int i = 0; i < 6; i++) {
			bindings.position(i);
			bindings.binding(i);
			bindings.stride(0);
			bindings.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
		}
		inputState.pVertexBindingDescriptions(bindings);
		VkVertexInputAttributeDescription.Buffer inputs = VkVertexInputAttributeDescription.mallocStack(6,stack);
		//TODO: implement attributes//
		return inputState;
	}

	@Override
	protected VkPipelineShaderStageCreateInfo.Buffer genPipelineStages(MemoryStack stack) {
		VkPipelineShaderStageCreateInfo.Buffer shaders = VkPipelineShaderStageCreateInfo.callocStack(1,stack);
		return null;
	}

	@Override
	protected VkPipelineViewportStateCreateInfo genViewportState(MemoryStack stack) {
		VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.mallocStack(stack);
		viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
		viewportState.pNext(VK_NULL_HANDLE);
		viewportState.flags(0);
		VkViewport.Buffer viewports = VkViewport.mallocStack(1,stack);
		viewports.x(0);
		viewports.y(0);
		viewports.width(ClientInput.currentWindowWidth.get());
		viewports.height(ClientInput.currentWindowWidth.get());
		viewports.minDepth(0);
		viewports.maxDepth(1);
		viewportState.pViewports(viewports);
		VkRect2D.Buffer scissors = VkRect2D.mallocStack(1,stack);
		VkOffset2D scissorOffset = VkOffset2D.mallocStack(stack);
		scissorOffset.set(0,0);
		scissors.offset(scissorOffset);
		scissors.extent(deviceState.swapExtent);
		return viewportState;
	}

	@Override
	protected VkPipelineTessellationStateCreateInfo genTessellationState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineColorBlendStateCreateInfo genColourBlendState(MemoryStack stack) {
		VkPipelineColorBlendStateCreateInfo blendState = VkPipelineColorBlendStateCreateInfo.mallocStack(stack);
		blendState.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
		blendState.pNext(VK_NULL_HANDLE);
		blendState.flags(0);
		blendState.logicOpEnable(false);
		blendState.logicOp(VK_LOGIC_OP_EQUIVALENT);
		VkPipelineColorBlendAttachmentState.Buffer attachments = VkPipelineColorBlendAttachmentState.mallocStack(4,stack);
		attachments.position(0);
		attachments.position(1);
		attachments.position(2);
		attachments.position(3);
		//TODO: WHAT ARE THE ATTACHMENTS//
		//FIXME: IMPLEMENT
		attachments.position(0);
		blendState.pAttachments(attachments);
		blendState.blendConstants(stack.floats(0.F,0.F,0.F,0.F));
		return blendState;
	}

	@Override
	protected VkPipelineDepthStencilStateCreateInfo genDepthStencilState(MemoryStack stack) {
		VkPipelineDepthStencilStateCreateInfo stencilState = VkPipelineDepthStencilStateCreateInfo.mallocStack(stack);
		stencilState.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
		stencilState.pNext(VK_NULL_HANDLE);
		stencilState.flags(0);
		stencilState.depthTestEnable(true);
		stencilState.depthWriteEnable(true);
		stencilState.depthCompareOp(VK_COMPARE_OP_LESS);
		stencilState.depthBoundsTestEnable(true);
		stencilState.stencilTestEnable(false);
		VkStencilOpState frontOp = VkStencilOpState.mallocStack(stack);
		frontOp.set(VK_STENCIL_OP_KEEP,VK_STENCIL_OP_KEEP,VK_STENCIL_OP_KEEP,VK_COMPARE_OP_ALWAYS,
				0,0,0);
		stencilState.front(frontOp);
		stencilState.back(frontOp);
		stencilState.minDepthBounds(0);
		stencilState.maxDepthBounds(1);
		return stencilState;
	}
}
