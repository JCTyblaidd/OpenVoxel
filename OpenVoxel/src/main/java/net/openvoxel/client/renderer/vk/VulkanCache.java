package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.client.renderer.vk.pipeline.VulkanShaderModule;
import net.openvoxel.client.renderer.vk.pipeline.impl.VulkanGuiPipeline;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Stores all vulkan pipelines, render passes, pipeline layouts
 *  that could be used by vulkan
 */
public class VulkanCache {

	//Immutable Samplers...

	//Shaders...
	public VulkanShaderModule SHADER_GUI_STANDARD;

	//Descriptor Set Layouts...
	public long DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY;
	//DESCRIPTOR_SET_LAYOUT_WORLD_CONSTANTS

	//Pipeline Layouts...
	public long PIPELINE_LAYOUT_GUI_STANDARD_INPUT;

	//Render Passes...
	//public VulkanRenderPass RENDER_PASS_VOXEL_GENERATE;
	//public VulkanRenderPass RENDER_PASS_VOXEL_G_BUFFER;
	//public VulkanRenderPass RENDER_PASS_VOXEL_FINAL;
	public VulkanRenderPass RENDER_PASS_FORWARD_ONLY;

	//Graphics Pipelines...
	public VulkanGuiPipeline PIPELINE_FORWARD_GUI;

	public void LoadSingle(VulkanState state_handle) {
		VulkanRenderPass.LoadFormats(state_handle);
		VulkanDevice device_handle = state_handle.VulkanDevice;
		VkDevice device = device_handle.logicalDevice;
		long pipelineCache = VK_NULL_HANDLE;
		try(MemoryStack stack = MemoryStack.stackPush()) {
			//Immutable Samplers

			//Load Shaders
			SHADER_GUI_STANDARD = CreateShader("GUI_STANDARD", "gui/guiShader");
			SHADER_GUI_STANDARD.loadModules(device_handle,new ArrayList<>());

			//Descriptor Set Layouts
			DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY = CreateDescriptorLayout(device,stack,0);

			//Pipeline Layouts
			PIPELINE_LAYOUT_GUI_STANDARD_INPUT =
					CreatePipelineLayout(device,stack, 1,
							DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY);

			//Render Passes
			RENDER_PASS_FORWARD_ONLY = new VulkanRenderPass(VulkanRenderPass.RENDER_PASS_TYPE_FORWARD);
			RENDER_PASS_FORWARD_ONLY.generate(device);

			//Graphics Pipelines
			PIPELINE_FORWARD_GUI = new VulkanGuiPipeline(SHADER_GUI_STANDARD);
			PIPELINE_FORWARD_GUI.generate(device,PIPELINE_LAYOUT_GUI_STANDARD_INPUT,
					RENDER_PASS_FORWARD_ONLY.RenderPass,0,
					VK_NULL_HANDLE,VK_NULL_HANDLE);

			//Unload Shaders
			SHADER_GUI_STANDARD.unloadModules(device);
		}
	}

	public void ReloadShaders(VulkanDevice device) {
		VulkanUtility.LogWarn("Shader Reload not yet Implemented");
	}


	public void FreeSingle(VkDevice device) {

		//Graphics Pipelines
		PIPELINE_FORWARD_GUI.free(device);

		//Render Passes
		RENDER_PASS_FORWARD_ONLY.free(device);

		//Pipeline Layouts
		vkDestroyPipelineLayout(device,PIPELINE_LAYOUT_GUI_STANDARD_INPUT,null);

		//Descriptor Set Layouts
		vkDestroyDescriptorSetLayout(device,DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY,null);

		//Immutable Samplers
	}

	private VulkanShaderModule CreateShader(String id,String path) {
		return new VulkanShaderModule(id,ResourceManager.getResource(ResourceType.SHADER,path));
	}

	private long CreatePipelineLayout(VkDevice device, MemoryStack old_stack,int layoutID,long... descriptorSets) {
		try(MemoryStack stack = old_stack.push()) {
			VkPipelineLayoutCreateInfo createInfo = VkPipelineLayoutCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.pSetLayouts(descriptorSets.length == 0 ? null : stack.longs(descriptorSets));
			if(layoutID == 0) {
				//No Push Constants
				createInfo.pPushConstantRanges(null);
			}else if(layoutID == 1){
				//GUI image_offset & use_texture
				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.mallocStack(1,stack);
				pushConstants.position(0);
				pushConstants.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
				pushConstants.offset(0);
				pushConstants.size(2 * 4);
				createInfo.pPushConstantRanges(pushConstants);
			}else{
				throw new RuntimeException("Unknown ID");
			}
			LongBuffer returnVal = stack.mallocLong(1);
			int vkResult = vkCreatePipelineLayout(device,createInfo,null,returnVal);
			if(vkResult == VK_SUCCESS) {
				return returnVal.get(0);
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create pipeline layout",vkResult);
				return VK_NULL_HANDLE;
			}
		}
	}

	private long CreateDescriptorLayout(VkDevice device, MemoryStack old_stack,int layoutID) {
		try(MemoryStack stack = old_stack.push()) {
			VkDescriptorSetLayoutCreateInfo createInfo = VkDescriptorSetLayoutCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			if(layoutID == 0) {
				//GUI Texture Array
				VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.mallocStack(1,stack);
				bindings.position(0);
				bindings.binding(0);
				bindings.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
				bindings.descriptorCount(32);
				bindings.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
				bindings.pImmutableSamplers(null);
				createInfo.pBindings(bindings);
			}else{
				throw new RuntimeException("Unknown ID");
			}
			LongBuffer returnVal = stack.mallocLong(1);
			int vkResult = vkCreateDescriptorSetLayout(device,createInfo,null,returnVal);
			if(vkResult == VK_SUCCESS) {
				return returnVal.get(0);
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create descriptor set layout",vkResult);
				return VK_NULL_HANDLE;
			}
		}
	}
}
