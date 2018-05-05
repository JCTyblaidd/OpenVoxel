package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.client.renderer.vk.pipeline.VulkanShaderModule;
import net.openvoxel.client.renderer.vk.pipeline.impl.VulkanGuiPipeline;
import net.openvoxel.client.renderer.vk.pipeline.impl.VulkanWorldForwardPipeline;
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

	//Immutable Images...
	public long IMAGE_BLOCK_ATLAS = 0;

	//Immutable Samplers...
	public long SAMPLER_BLOCK_ATLAS = 0;

	//Shaders...
	public VulkanShaderModule SHADER_GUI_STANDARD;
	public VulkanShaderModule SHADER_GUI_TEXT;
	public VulkanShaderModule SHADER_WORLD_FORWARD;

	//Descriptor Set Layouts...
	public long DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY;
	public long DESCRIPTOR_SET_WORLD_CONSTANTS;

	//Pipeline Layouts...
	public long PIPELINE_LAYOUT_GUI_STANDARD_INPUT;
	public long PIPELINE_LAYOUT_WORLD_STANDARD_INPUT;

	//Render Passes...
	//public VulkanRenderPass RENDER_PASS_VOXEL_GENERATE;
	//public VulkanRenderPass RENDER_PASS_VOXEL_G_BUFFER;
	//public VulkanRenderPass RENDER_PASS_VOXEL_FINAL;
	public VulkanRenderPass RENDER_PASS_FORWARD_ONLY;

	//Graphics Pipelines...
	public VulkanGuiPipeline PIPELINE_FORWARD_GUI;
	public VulkanGuiPipeline PIPELINE_FORWARD_TEXT;
	public VulkanWorldForwardPipeline PIPELINE_FORWARD_WORLD;

	public void LoadSingle(VulkanState state_handle) {
		VulkanRenderPass.LoadFormats(state_handle);
		VulkanDevice device_handle = state_handle.VulkanDevice;
		VkDevice device = device_handle.logicalDevice;
		long pipelineCache = VK_NULL_HANDLE;
		try(MemoryStack stack = MemoryStack.stackPush()) {
			//Immutable Images...

			//Immutable Samplers...
			SAMPLER_BLOCK_ATLAS = CreateImmutableSampler(device_handle,stack,16);//TODO: MAX LOD (choose correct)

			//Load Shaders
			SHADER_GUI_STANDARD = CreateShader("GUI_STANDARD", "gui/guiShader");
			SHADER_GUI_STANDARD.loadModules(device_handle,new ArrayList<>());
			SHADER_GUI_TEXT = CreateShader("GUI_TEXT","gui/textShader");
			SHADER_GUI_TEXT.loadModules(device_handle,new ArrayList<>());
			SHADER_WORLD_FORWARD = CreateShader("WORLD_FORWARD","world/forward/worldOpaque");
			SHADER_WORLD_FORWARD.loadModules(device_handle,new ArrayList<>());

			//Descriptor Set Layouts
			DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY = CreateDescriptorLayout(device,stack,0);
			DESCRIPTOR_SET_WORLD_CONSTANTS = CreateDescriptorLayout(device,stack,1);

			//Pipeline Layouts
			PIPELINE_LAYOUT_GUI_STANDARD_INPUT =
					CreatePipelineLayout(device,stack, 1,
							DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY);
			PIPELINE_LAYOUT_WORLD_STANDARD_INPUT =
					CreatePipelineLayout(device,stack,2,
							DESCRIPTOR_SET_WORLD_CONSTANTS);

			//Render Passes
			RENDER_PASS_FORWARD_ONLY = new VulkanRenderPass(VulkanRenderPass.RENDER_PASS_TYPE_FORWARD);
			RENDER_PASS_FORWARD_ONLY.generate(device);

			//Graphics Pipelines
			PIPELINE_FORWARD_GUI = new VulkanGuiPipeline(SHADER_GUI_STANDARD,false);
			PIPELINE_FORWARD_GUI.generate(device,PIPELINE_LAYOUT_GUI_STANDARD_INPUT,
					RENDER_PASS_FORWARD_ONLY.RenderPass,0,
					VK_NULL_HANDLE,VK_NULL_HANDLE);
			PIPELINE_FORWARD_TEXT = new VulkanGuiPipeline(SHADER_GUI_TEXT,true);
			PIPELINE_FORWARD_TEXT.generate(device,PIPELINE_LAYOUT_GUI_STANDARD_INPUT,
					RENDER_PASS_FORWARD_ONLY.RenderPass,0,
					VK_NULL_HANDLE,PIPELINE_FORWARD_GUI.getPipeline());
			PIPELINE_FORWARD_WORLD = new VulkanWorldForwardPipeline(SHADER_WORLD_FORWARD);
			//PIPELINE_FORWARD_WORLD.generate(device,PIPELINE_LAYOUT_WORLD_STANDARD_INPUT,
			//		RENDER_PASS_FORWARD_ONLY.RenderPass,0,
			//		VK_NULL_HANDLE,VK_NULL_HANDLE);

			//Unload Shaders
			SHADER_WORLD_FORWARD.unloadModules(device);
			SHADER_GUI_TEXT.unloadModules(device);
			SHADER_GUI_STANDARD.unloadModules(device);
		}
	}

	public void ReloadShaders(VulkanDevice device) {
		VulkanUtility.LogWarn("Shader Reload not yet Implemented");
	}


	public void FreeSingle(VkDevice device) {

		//Graphics Pipelines
		//PIPELINE_FORWARD_WORLD.free(device);
		PIPELINE_FORWARD_TEXT.free(device);
		PIPELINE_FORWARD_GUI.free(device);

		//Render Passes
		RENDER_PASS_FORWARD_ONLY.free(device);

		//Pipeline Layouts
		vkDestroyPipelineLayout(device,PIPELINE_LAYOUT_WORLD_STANDARD_INPUT,null);
		vkDestroyPipelineLayout(device,PIPELINE_LAYOUT_GUI_STANDARD_INPUT,null);

		//Descriptor Set Layouts
		vkDestroyDescriptorSetLayout(device,DESCRIPTOR_SET_WORLD_CONSTANTS,null);
		vkDestroyDescriptorSetLayout(device,DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY,null);

		//Immutable Samplers
		vkDestroySampler(device,SAMPLER_BLOCK_ATLAS,null);
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
			}else if(layoutID == 2) {
				//World Chunk Renderer...
				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.mallocStack(1,stack);
				pushConstants.position(0);
				pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
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
				{
					bindings.binding(0);
					bindings.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
					bindings.descriptorCount(32);
					bindings.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
					bindings.pImmutableSamplers(null);
				}
				bindings.position(0);
				createInfo.pBindings(bindings);
			}else if(layoutID == 1) {
				//World Constant Array
				VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.mallocStack(2,stack);
				bindings.position(0);
				{
					bindings.binding(0);
					bindings.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
					bindings.descriptorCount(3);
					bindings.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);
					bindings.pImmutableSamplers(stack.longs(SAMPLER_BLOCK_ATLAS));
				}
				bindings.position(1);
				{
					bindings.binding(1);
					bindings.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
					bindings.descriptorCount(1);
					bindings.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);
					bindings.pImmutableSamplers(null);
				}
				bindings.position(0);
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

	private long CreateImmutableSampler(VulkanDevice device,MemoryStack old_stack,int mip_levels) {
		try(MemoryStack stack = old_stack.push()) {
			VkSamplerCreateInfo imageSampler = VkSamplerCreateInfo.mallocStack(stack);
			imageSampler.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
			imageSampler.pNext(VK_NULL_HANDLE);
			imageSampler.flags(0);
			imageSampler.magFilter(VK_FILTER_LINEAR);
			imageSampler.minFilter(VK_FILTER_LINEAR);
			imageSampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
			imageSampler.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			imageSampler.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			imageSampler.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			imageSampler.mipLodBias(0.0f);
			if(device.features.samplerAnisotropy()) {
				imageSampler.anisotropyEnable(true);
				imageSampler.maxAnisotropy(device.properties.limits().maxSamplerAnisotropy());
			}else {
				imageSampler.anisotropyEnable(false);
				imageSampler.maxAnisotropy(1.0f);
			}
			imageSampler.compareEnable(false);
			imageSampler.compareOp(VK_COMPARE_OP_ALWAYS);
			imageSampler.minLod(0.0f);
			imageSampler.maxLod((float)mip_levels);
			imageSampler.borderColor(0);
			imageSampler.unnormalizedCoordinates(false);

			LongBuffer pSampler = stack.mallocLong(1);
			int vkResult = vkCreateSampler(device.logicalDevice,imageSampler,null,pSampler);
			if(vkResult == VK_SUCCESS) {
				return pSampler.get(0);
			}else{
				VulkanUtility.CrashOnBadResult("Failed to create immutable sampler",vkResult);
				return VK_NULL_HANDLE;
			}
		}
	}
}
