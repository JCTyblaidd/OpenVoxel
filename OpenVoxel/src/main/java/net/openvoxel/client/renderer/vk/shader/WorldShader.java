package net.openvoxel.client.renderer.vk.shader;

import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.common.resources.ResourceHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

/**
 * Created by James on 17/04/2017.
 *
 * World Input Shader
 */
public abstract class WorldShader extends AbstractVkShaderPipeline{


	public WorldShader(VkDeviceState state, ResourceHandle handle) {
		super(state, handle);
	}


	@Override
	protected VkPipelineDynamicStateCreateInfo genDynamicState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineMultisampleStateCreateInfo genMultiSampleState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineRasterizationStateCreateInfo genRasterizationState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineVertexInputStateCreateInfo genVertexInput(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineShaderStageCreateInfo.Buffer genPipelineStages(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineViewportStateCreateInfo genViewportState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineTessellationStateCreateInfo genTessellationState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineColorBlendStateCreateInfo genColourBlendState(MemoryStack stack) {
		return null;
	}

	@Override
	protected VkPipelineDepthStencilStateCreateInfo genDepthStencilState(MemoryStack stack) {
		return null;
	}
}
