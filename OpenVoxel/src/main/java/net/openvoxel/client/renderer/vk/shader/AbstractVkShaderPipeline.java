package net.openvoxel.client.renderer.vk.shader;

import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.common.resources.ResourceHandle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 14/04/2017.
 *
 * Vulkan Shader Pipeline
 */
public abstract class AbstractVkShaderPipeline {

	private VkDeviceState deviceState;
	private ResourceHandle resourceHandle;
	protected long pipelineHandle;

	public long getPipeline() {
		return pipelineHandle;
	}

	public AbstractVkShaderPipeline(VkDeviceState state, ResourceHandle handle) {
		deviceState = state;
		resourceHandle = handle;
		generatePipeline();
	}

	public boolean needsRegen() {
		return resourceHandle.checkIfChanged();
	}

	public void regenerate() {
		if(resourceHandle.checkIfChanged()) {
			vkDestroyPipeline(deviceState.renderDevice.device,pipelineHandle,null);
			generatePipeline();
		}
	}

	protected abstract void generatePipeline();

	protected void genPipelineObject() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkGraphicsPipelineCreateInfo.Buffer createInfo = VkGraphicsPipelineCreateInfo.callocStack(1);
			createInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
			createInfo.pNext(0);
			createInfo.flags(getPipelineFlags());
			createInfo.pStages(genPipelineStages(stack));
			createInfo.pVertexInputState(genVertexInput(stack));
			createInfo.pInputAssemblyState(genInputAssembly(stack));
			createInfo.pTessellationState(genTessellationState(stack));
			createInfo.pViewportState(genViewportState(stack));
			createInfo.pRasterizationState(genRasterizationState(stack));
			createInfo.pMultisampleState(genMultiSampleState(stack));
			createInfo.pDepthStencilState(genDepthStencilState(stack));
			createInfo.pColorBlendState(genColourBlendState(stack));
			createInfo.pDynamicState(genDynamicState(stack));
			LongBuffer retBuffer = stack.callocLong(1);
			vkCreateGraphicsPipelines(deviceState.renderDevice.device,0,createInfo,null,retBuffer);
			pipelineHandle = retBuffer.get(0);
		}
	}

	protected abstract VkPipelineDynamicStateCreateInfo genDynamicState(MemoryStack stack);
	protected abstract VkPipelineMultisampleStateCreateInfo genMultiSampleState(MemoryStack stack);
	protected abstract VkPipelineRasterizationStateCreateInfo genRasterizationState(MemoryStack stack);
	protected abstract VkPipelineVertexInputStateCreateInfo genVertexInput(MemoryStack stack);
	protected abstract VkPipelineShaderStageCreateInfo.Buffer genPipelineStages(MemoryStack stack);
	protected abstract VkPipelineViewportStateCreateInfo genViewportState(MemoryStack stack);
	protected abstract VkPipelineTessellationStateCreateInfo genTessellationState(MemoryStack stack);
	protected abstract VkPipelineColorBlendStateCreateInfo genColourBlendState(MemoryStack stack);
	protected abstract VkPipelineDepthStencilStateCreateInfo genDepthStencilState(MemoryStack stack);

	protected VkPipelineInputAssemblyStateCreateInfo genInputAssembly(MemoryStack stack) {
		VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
		inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
		inputAssembly.pNext(0);
		inputAssembly.flags(0);
		inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
		inputAssembly.primitiveRestartEnable(false);
		return inputAssembly;
	}

	protected int getPipelineFlags() {
		return 0;
	}


	private long createModule(ByteBuffer code) {
		long module;
		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
			createInfo.pNext(0);
			createInfo.flags(0);
			createInfo.pCode(code);
			LongBuffer moduleHandle = stack.callocLong(1);
			deviceState.success(vkCreateShaderModule(deviceState.renderDevice.device,createInfo,null,moduleHandle),"Error Creating Shader Module");
			module = moduleHandle.get(0);
		}
		return module;
	}

}
