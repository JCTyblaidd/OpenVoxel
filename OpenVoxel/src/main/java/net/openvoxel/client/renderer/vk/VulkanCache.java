package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.procedure.TLongProcedure;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import net.openvoxel.client.renderer.vk.pipeline.VulkanShaderModule;
import net.openvoxel.client.renderer.vk.pipeline.impl.VulkanGuiPipeline;
import net.openvoxel.client.renderer.vk.pipeline.impl.VulkanWorldForwardPipeline;
import net.openvoxel.client.textureatlas.ArrayAtlas;
import net.openvoxel.client.textureatlas.BaseAtlas;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.common.resources.ResourceType;
import net.openvoxel.utility.MathUtilities;
import net.openvoxel.utility.debug.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Stores all vulkan pipelines, render passes, pipeline layouts
 *  that could be used by vulkan
 */
public class VulkanCache {

	//Configuration [TODO: IMPLEMENT!]
	//private int config_atlasMipLimit = 6;
	//TODO: REWRITE Texture Manager! {
	//private float config_anisotropy_limit = 0.F;//DISABLED [CAUSES BLEEDING]

	//Immutable Images...
	@Deprecated
	public long IMAGE_BLOCK_ATLAS_ARRAY;

	//Immutable Image Memory...
	@Deprecated
	public long IMAGE_MEMORY_ATLAS_ARRAY;

	//Immutable Image Views...
	@Deprecated
	public long IMAGE_VIEW_BLOCK_ATLAS;

	//Immutable Samplers...
	@Deprecated
	public long SAMPLER_BLOCK_ATLAS;

	//Immutable Image Atlas
	public TLongList IMAGE_ATLAS_DIFFUSE_LIST = new TLongArrayList();
	public TLongList IMAGE_ATLAS_NORMAL_LIST = new TLongArrayList();
	public TLongList IMAGE_ATLAS_PBR_LIST = new TLongArrayList();
	public TLongList IMAGE_VIEW_ATLAS_DIFFUSE_LIST = new TLongArrayList();
	public TLongList IMAGE_VIEW_ATLAS_NORMAL_LIST = new TLongArrayList();
	public TLongList IMAGE_VIEW_ATLAS_PBR_LIST = new TLongArrayList();
	public TLongList IMAGE_MEMORY_ATLAS_DIFFUSE_LIST = new TLongArrayList();
	public TLongList IMAGE_MEMORY_ATLAS_NORMAL_LIST = new TLongArrayList();
	public TLongList IMAGE_MEMORY_ATLAS_PBR_LIST = new TLongArrayList();
	public long IMAGE_ATLAS_SAMPLER = VK_NULL_HANDLE;

	//Immutable Descriptor Pools...
	public long DESCRIPTOR_POOL_ATLAS;
	public long DESCRIPTOR_SET_ATLAS;

	//Shaders...
	public VulkanShaderModule SHADER_GUI_STANDARD;
	public VulkanShaderModule SHADER_GUI_TEXT;
	public VulkanShaderModule SHADER_WORLD_FORWARD;

	//Descriptor Set Layouts...
	public long DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY;
	public long DESCRIPTOR_SET_LAYOUT_WORLD_ATLAS;
	public long DESCRIPTOR_SET_LAYOUT_WORLD_CONSTANTS;

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
		try(MemoryStack stack = stackPush()) {
			//Immutable Samplers...
			//SAMPLER_BLOCK_ATLAS = CreateImmutableSampler(device_handle,stack,config_atlasMipLimit,config_anisotropy_limit);

			//Immutable Descriptor Pool...
			DESCRIPTOR_POOL_ATLAS = CreateDescriptorPool(device,stack);

			//Load Shaders
			SHADER_GUI_STANDARD = CreateShader("GUI_STANDARD", "gui/guiShader");
			SHADER_GUI_STANDARD.loadModules(device_handle,new ArrayList<>());
			SHADER_GUI_TEXT = CreateShader("GUI_TEXT","gui/textShader");
			SHADER_GUI_TEXT.loadModules(device_handle,new ArrayList<>());
			SHADER_WORLD_FORWARD = CreateShader("WORLD_FORWARD","world/forward/worldOpaque");
			SHADER_WORLD_FORWARD.loadModules(device_handle,new ArrayList<>());

			//Descriptor Set Layouts
			DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY = CreateDescriptorLayout(device,stack,0);
			DESCRIPTOR_SET_LAYOUT_WORLD_ATLAS = CreateDescriptorLayout(device,stack,1);
			DESCRIPTOR_SET_LAYOUT_WORLD_CONSTANTS = CreateDescriptorLayout(device,stack,2);


			//Immutable Descriptor Set....
			DESCRIPTOR_SET_ATLAS = CreateDescriptorSet(device,stack);

			//Pipeline Layouts
			PIPELINE_LAYOUT_GUI_STANDARD_INPUT =
					CreatePipelineLayout(device,stack, 1,
							DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY);
			PIPELINE_LAYOUT_WORLD_STANDARD_INPUT =
					CreatePipelineLayout(device,stack,2,
							DESCRIPTOR_SET_LAYOUT_WORLD_CONSTANTS,
							DESCRIPTOR_SET_LAYOUT_WORLD_ATLAS);

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
			PIPELINE_FORWARD_WORLD.generate(device,PIPELINE_LAYOUT_WORLD_STANDARD_INPUT,
					RENDER_PASS_FORWARD_ONLY.RenderPass,0,
					VK_NULL_HANDLE,VK_NULL_HANDLE);

			//Unload Shaders
			SHADER_WORLD_FORWARD.unloadModules(device);
			SHADER_GUI_TEXT.unloadModules(device);
			SHADER_GUI_STANDARD.unloadModules(device);
		}
	}

	//TODO: IMPLEMENT SHADER & IMAGE RELOADING...


	public void FreeSingle(VkDevice device) {

		//Graphics Pipelines
		PIPELINE_FORWARD_WORLD.free(device);
		PIPELINE_FORWARD_TEXT.free(device);
		PIPELINE_FORWARD_GUI.free(device);

		//Render Passes
		RENDER_PASS_FORWARD_ONLY.free(device);

		//Pipeline Layouts
		vkDestroyPipelineLayout(device, PIPELINE_LAYOUT_WORLD_STANDARD_INPUT, null);
		vkDestroyPipelineLayout(device, PIPELINE_LAYOUT_GUI_STANDARD_INPUT, null);

		//Descriptor Set Layouts
		vkDestroyDescriptorSetLayout(device, DESCRIPTOR_SET_LAYOUT_WORLD_CONSTANTS, null);
		vkDestroyDescriptorSetLayout(device, DESCRIPTOR_SET_LAYOUT_WORLD_ATLAS, null);
		vkDestroyDescriptorSetLayout(device, DESCRIPTOR_SET_LAYOUT_GUI_TEXTURE_ARRAY, null);

		//Immutable Descriptor Pool
		vkDestroyDescriptorPool(device,DESCRIPTOR_POOL_ATLAS,null);

		//Immutable Samplers
		//vkDestroySampler(device, SAMPLER_BLOCK_ATLAS, null);

	}

	public void DestroyAtlasArray(VkDevice device, VulkanMemory memory) {

		vkDestroySampler(device,IMAGE_ATLAS_SAMPLER,null);

		TLongProcedure DESTROY_IMAGE_VIEW = view -> {
			vkDestroyImageView(device,view,null);
			return true;
		};
		TLongProcedure DESTROY_IMAGE = image -> {
			vkDestroyImage(device,image,null);
			return true;
		};
		TLongProcedure FREE_MEMORY = mem -> {
			memory.freeDedicatedMemory(mem);
			return true;
		};

		IMAGE_VIEW_ATLAS_DIFFUSE_LIST.forEach(DESTROY_IMAGE_VIEW);
		IMAGE_VIEW_ATLAS_NORMAL_LIST.forEach(DESTROY_IMAGE_VIEW);
		IMAGE_VIEW_ATLAS_PBR_LIST.forEach(DESTROY_IMAGE_VIEW);
		IMAGE_VIEW_ATLAS_DIFFUSE_LIST.clear();
		IMAGE_VIEW_ATLAS_NORMAL_LIST.clear();
		IMAGE_VIEW_ATLAS_PBR_LIST.clear();

		IMAGE_ATLAS_DIFFUSE_LIST.forEach(DESTROY_IMAGE);
		IMAGE_ATLAS_NORMAL_LIST.forEach(DESTROY_IMAGE);
		IMAGE_ATLAS_PBR_LIST.forEach(DESTROY_IMAGE);
		IMAGE_ATLAS_DIFFUSE_LIST.clear();
		IMAGE_ATLAS_PBR_LIST.clear();
		IMAGE_ATLAS_NORMAL_LIST.clear();

		IMAGE_MEMORY_ATLAS_DIFFUSE_LIST.forEach(FREE_MEMORY);
		IMAGE_MEMORY_ATLAS_NORMAL_LIST.forEach(FREE_MEMORY);
		IMAGE_MEMORY_ATLAS_PBR_LIST.forEach(FREE_MEMORY);
		IMAGE_MEMORY_ATLAS_DIFFUSE_LIST.clear();
		IMAGE_MEMORY_ATLAS_NORMAL_LIST.clear();
		IMAGE_MEMORY_ATLAS_PBR_LIST.clear();
	}

	public void LoadAtlasArray(VulkanDevice device, VulkanMemory memory, ArrayAtlas atlas, VulkanCommandHandler command, int maxSize, int maxMip, float anisotropy) {
		try(MemoryStack stack = stackPush()) {
			int arrayCount = atlas.generateArrays(
					device.properties.limits().maxImageArrayLayers(),
					Math.min(maxSize,device.properties.limits().maxImageDimension2D())
			);
			if(arrayCount > 64) {//TODO: MAX_IMAGE_ARRAY_COUNT
				VulkanUtility.CrashOnBadResult("Too Many Array Layers",-1);
			}
			for(int i = 0; i < arrayCount; i++) {
				IMAGE_VIEW_ATLAS_DIFFUSE_LIST.add(VK_NULL_HANDLE);
				IMAGE_VIEW_ATLAS_NORMAL_LIST.add(VK_NULL_HANDLE);
				IMAGE_VIEW_ATLAS_PBR_LIST.add(VK_NULL_HANDLE);
				IMAGE_ATLAS_DIFFUSE_LIST.add(VK_NULL_HANDLE);
				IMAGE_ATLAS_NORMAL_LIST.add(VK_NULL_HANDLE);
				IMAGE_ATLAS_PBR_LIST.add(VK_NULL_HANDLE);
				IMAGE_MEMORY_ATLAS_DIFFUSE_LIST.add(VK_NULL_HANDLE);
				IMAGE_MEMORY_ATLAS_NORMAL_LIST.add(VK_NULL_HANDLE);
				IMAGE_MEMORY_ATLAS_PBR_LIST.add(VK_NULL_HANDLE);
			}

			//Descriptor sets for updating
			VkDescriptorImageInfo.Buffer descriptorDiffuse = VkDescriptorImageInfo.mallocStack(arrayCount,stack);
			VkDescriptorImageInfo.Buffer descriptorNormal  = VkDescriptorImageInfo.mallocStack(arrayCount,stack);
			VkDescriptorImageInfo.Buffer descriptorPBR     = VkDescriptorImageInfo.mallocStack(arrayCount,stack);
			VkDescriptorImageInfo.Buffer descriptorSampler = VkDescriptorImageInfo.mallocStack(1,stack);

			//Descriptor Update Call
			VkWriteDescriptorSet.Buffer writeDescriptor = VkWriteDescriptorSet.mallocStack(4,stack);
			{
				writeDescriptor.position(0);
				writeDescriptor.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
				writeDescriptor.pNext(VK_NULL_HANDLE);
				writeDescriptor.dstSet(DESCRIPTOR_SET_ATLAS);
				writeDescriptor.dstBinding(0);
				writeDescriptor.dstArrayElement(0);
				writeDescriptor.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE);
				writeDescriptor.pImageInfo(descriptorDiffuse);
			}
			{
				writeDescriptor.position(1);
				writeDescriptor.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
				writeDescriptor.pNext(VK_NULL_HANDLE);
				writeDescriptor.dstSet(DESCRIPTOR_SET_ATLAS);
				writeDescriptor.dstBinding(1);
				writeDescriptor.dstArrayElement(0);
				writeDescriptor.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE);
				writeDescriptor.pImageInfo(descriptorNormal);
			}
			{
				writeDescriptor.position(2);
				writeDescriptor.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
				writeDescriptor.pNext(VK_NULL_HANDLE);
				writeDescriptor.dstSet(DESCRIPTOR_SET_ATLAS);
				writeDescriptor.dstBinding(2);
				writeDescriptor.dstArrayElement(0);
				writeDescriptor.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE);
				writeDescriptor.pImageInfo(descriptorPBR);
			}
			{
				writeDescriptor.position(3);
				writeDescriptor.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
				writeDescriptor.pNext(VK_NULL_HANDLE);
				writeDescriptor.dstSet(DESCRIPTOR_SET_ATLAS);
				writeDescriptor.dstBinding(3);
				writeDescriptor.dstArrayElement(0);
				writeDescriptor.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER);
				writeDescriptor.pImageInfo(descriptorSampler);
			}
			writeDescriptor.position(0);

			//Uses Atomic to allow lambda reference
			AtomicInteger maxMipCount = new AtomicInteger(0);
			Validate.Condition(IMAGE_ATLAS_DIFFUSE_LIST.size() > 0,"Called Load Atlas with non empty atlas array");
			atlas.createImageArrays(new ArrayAtlas.ArrayLayerCallback() {
				@Override
				public void createArray(int arrayIndex,int imgLayerCount,int mipCount, int imgSize) {
					try(MemoryStack stack = stackPush()) {
						maxMipCount.set(Math.max(maxMipCount.get(),mipCount));

						//Create Images
						VkImageCreateInfo imageCreate = VkImageCreateInfo.mallocStack(stack);
						imageCreate.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
						imageCreate.pNext(VK_NULL_HANDLE);
						imageCreate.flags(0);
						imageCreate.imageType(VK_IMAGE_TYPE_2D);
						imageCreate.format(VulkanRenderPass.formatSimpleReadImage);
						imageCreate.extent().set(imgSize,imgSize,1);
						imageCreate.mipLevels(mipCount);
						imageCreate.arrayLayers(imgLayerCount);
						imageCreate.samples(VK_SAMPLE_COUNT_1_BIT);
						imageCreate.tiling(VK_IMAGE_TILING_OPTIMAL);
						imageCreate.usage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
						imageCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
						imageCreate.pQueueFamilyIndices(null);
						imageCreate.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

						LongBuffer pResult = stack.mallocLong(1);
						int vkResult = vkCreateImage(device.logicalDevice,imageCreate,null,pResult);
						VulkanUtility.ValidateSuccess("Failed to create atlas array",vkResult);
						long imgValDiff = pResult.get(0);
						IMAGE_ATLAS_DIFFUSE_LIST.set(arrayIndex,imgValDiff);

						vkResult = vkCreateImage(device.logicalDevice,imageCreate,null,pResult);
						VulkanUtility.ValidateSuccess("Failed to create atlas array",vkResult);
						long imgValNorm = pResult.get(0);
						IMAGE_ATLAS_NORMAL_LIST.set(arrayIndex,imgValNorm);

						vkResult = vkCreateImage(device.logicalDevice,imageCreate,null,pResult);
						VulkanUtility.ValidateSuccess("Failed to create atlas array",vkResult);
						long imgValPBR = pResult.get(0);
						IMAGE_ATLAS_PBR_LIST.set(arrayIndex,imgValPBR);


						//Bind Memory
						long imgMemoryDiff = memory.allocateDedicatedImage(imgValDiff,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
						IMAGE_MEMORY_ATLAS_DIFFUSE_LIST.set(arrayIndex,imgMemoryDiff);
						vkResult = vkBindImageMemory(device.logicalDevice,imgValDiff,imgMemoryDiff,0L);
						VulkanUtility.ValidateSuccess("Failed to bind atlas memory",vkResult);

						long imgMemoryNorm = memory.allocateDedicatedImage(imgValNorm,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
						IMAGE_MEMORY_ATLAS_NORMAL_LIST.set(arrayIndex,imgMemoryNorm);
						vkResult = vkBindImageMemory(device.logicalDevice,imgValNorm,imgMemoryNorm,0L);
						VulkanUtility.ValidateSuccess("Failed to bind atlas memory",vkResult);

						long imgMemoryPBR = memory.allocateDedicatedImage(imgValPBR,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
						IMAGE_MEMORY_ATLAS_PBR_LIST.set(arrayIndex,imgMemoryPBR);
						vkResult = vkBindImageMemory(device.logicalDevice,imgValPBR,imgMemoryPBR,0L);
						VulkanUtility.ValidateSuccess("Failed to bind atlas memory",vkResult);


						//Create Image View
						VkImageViewCreateInfo viewCreate = VkImageViewCreateInfo.mallocStack(stack);
						viewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
						viewCreate.pNext(VK_NULL_HANDLE);
						viewCreate.flags(0);
						viewCreate.image(VK_NULL_HANDLE);
						viewCreate.viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY);
						viewCreate.format(VulkanRenderPass.formatSimpleReadImage);
						viewCreate.components().set(
								VK_COMPONENT_SWIZZLE_IDENTITY,
								VK_COMPONENT_SWIZZLE_IDENTITY,
								VK_COMPONENT_SWIZZLE_IDENTITY,
								VK_COMPONENT_SWIZZLE_IDENTITY
						);
						viewCreate.subresourceRange().set(
								VK_IMAGE_ASPECT_COLOR_BIT,
								0,
								mipCount,
								0,
								imgLayerCount
						);

						viewCreate.image(imgValDiff);
						vkResult = vkCreateImageView(device.logicalDevice,viewCreate,null,pResult);
						VulkanUtility.ValidateSuccess("Failed to create atlas view",vkResult);
						long imgViewDiff = pResult.get(0);
						IMAGE_VIEW_ATLAS_DIFFUSE_LIST.set(arrayIndex,imgViewDiff);

						viewCreate.image(imgValNorm);
						vkResult = vkCreateImageView(device.logicalDevice,viewCreate,null,pResult);
						VulkanUtility.ValidateSuccess("Failed to create atlas view",vkResult);
						long imgViewNorm = pResult.get(0);
						IMAGE_VIEW_ATLAS_NORMAL_LIST.set(arrayIndex,imgViewNorm);

						viewCreate.image(imgValPBR);
						vkResult = vkCreateImageView(device.logicalDevice,viewCreate,null,pResult);
						VulkanUtility.ValidateSuccess("Failed to create atlas view",vkResult);
						long imgViewPBR = pResult.get(0);
						IMAGE_VIEW_ATLAS_PBR_LIST.set(arrayIndex,imgViewPBR);

						//Update Descriptor Sets
						VkDescriptorImageInfo descriptor;
						descriptor = descriptorDiffuse.get(arrayIndex);
						descriptor.sampler(VK_NULL_HANDLE);
						descriptor.imageView(imgViewDiff);
						descriptor.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

						descriptor = descriptorNormal.get(arrayIndex);
						descriptor.sampler(VK_NULL_HANDLE);
						descriptor.imageView(imgViewNorm);
						descriptor.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

						descriptor = descriptorPBR.get(arrayIndex);
						descriptor.sampler(VK_NULL_HANDLE);
						descriptor.imageView(imgViewPBR);
						descriptor.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
					}
				}

				@Override
				public void storeArray(int arrayIndex, int layerIndex, int mipIndex, int imgSize, ByteBuffer diffuse, ByteBuffer normal, ByteBuffer pbr) {
					command.SingleUseImagePopulate(
						IMAGE_ATLAS_DIFFUSE_LIST.get(arrayIndex),
						diffuse,
						imgSize,
						imgSize,
						layerIndex,
						mipIndex
					);
					command.SingleUseImagePopulate(
						IMAGE_ATLAS_NORMAL_LIST.get(arrayIndex),
						normal,
						imgSize,
						imgSize,
						layerIndex,
						mipIndex
					);
					command.SingleUseImagePopulate(
						IMAGE_ATLAS_PBR_LIST.get(arrayIndex),
						pbr,
						imgSize,
						imgSize,
						layerIndex,
						mipIndex
					);
				}
			},maxMip);

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
			if(device.features.samplerAnisotropy() && anisotropy > 0.0f) {
				float chosenAnisotropy = MathUtilities.clamp(
						device.properties.limits().maxSamplerAnisotropy(),
						1.0f,
						anisotropy
				);
				VulkanUtility.LogInfo("Block Atlas Anisotropy: Enabled ["+chosenAnisotropy+"]");
				imageSampler.anisotropyEnable(true);
				imageSampler.maxAnisotropy(chosenAnisotropy);
			}else {
				VulkanUtility.LogInfo("Block Atlas Anisotropy: Unavailable/Disabled");
				imageSampler.anisotropyEnable(false);
				imageSampler.maxAnisotropy(1.0f);
			}
			imageSampler.compareEnable(false);
			imageSampler.compareOp(VK_COMPARE_OP_ALWAYS);
			imageSampler.minLod(0.0f);
			imageSampler.maxLod((float)maxMipCount.get());
			imageSampler.borderColor(0);
			imageSampler.unnormalizedCoordinates(false);

			LongBuffer pSampler = stack.mallocLong(1);
			int vkResult = vkCreateSampler(device.logicalDevice,imageSampler,null,pSampler);
			VulkanUtility.ValidateSuccess("Failed to create atlas sampler",vkResult);
			IMAGE_ATLAS_SAMPLER = pSampler.get(0);

			//Update Descriptor Sets
			descriptorSampler.sampler(IMAGE_ATLAS_SAMPLER);
			descriptorSampler.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			descriptorSampler.imageView(VK_NULL_HANDLE);
			vkUpdateDescriptorSets(device.logicalDevice,writeDescriptor,null);
		}
	}

	@Deprecated
	public void DestroyAtlas(VkDevice device,VulkanMemory memory) {
		//Immutable Image Views
		vkDestroyImageView(device,IMAGE_VIEW_BLOCK_ATLAS,null);

		//Immutable Images
		vkDestroyImage(device,IMAGE_BLOCK_ATLAS_ARRAY,null);

		memory.freeDedicatedMemory(IMAGE_MEMORY_ATLAS_ARRAY);
	}


	@Deprecated
	public void LoadAtlas(VkDevice device, VulkanMemory memory, BaseAtlas baseAtlas,VulkanCommandHandler command) {
		try(MemoryStack stack = stackPush()) {
			VkImageCreateInfo imageCreate = VkImageCreateInfo.mallocStack(stack);
			imageCreate.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageCreate.pNext(VK_NULL_HANDLE);
			imageCreate.flags(0);
			imageCreate.imageType(VK_IMAGE_TYPE_2D);
			imageCreate.format(VulkanRenderPass.formatSimpleReadImage);
			imageCreate.extent().set(baseAtlas.AtlasWidth,baseAtlas.AtlasHeight,1);
			imageCreate.mipLevels(baseAtlas.AtlasMipLevels);
			imageCreate.arrayLayers(3);
			imageCreate.samples(VK_SAMPLE_COUNT_1_BIT);
			imageCreate.tiling(VK_IMAGE_TILING_OPTIMAL);
			imageCreate.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
			imageCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			imageCreate.pQueueFamilyIndices(null);
			imageCreate.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateImage(device,imageCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create atlas image",vkResult);
			IMAGE_BLOCK_ATLAS_ARRAY = pReturn.get(0);

			IMAGE_MEMORY_ATLAS_ARRAY = memory.allocateDedicatedImage(
					IMAGE_BLOCK_ATLAS_ARRAY,
					VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
			);

			vkResult = vkBindImageMemory(device,IMAGE_BLOCK_ATLAS_ARRAY,IMAGE_MEMORY_ATLAS_ARRAY,0);
			VulkanUtility.ValidateSuccess("Failed to bind memory for atlas image",vkResult);


			VkImageViewCreateInfo viewCreate = VkImageViewCreateInfo.mallocStack(stack);
			viewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			viewCreate.pNext(VK_NULL_HANDLE);
			viewCreate.flags(0);
			viewCreate.image(IMAGE_BLOCK_ATLAS_ARRAY);
			viewCreate.viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY);
			viewCreate.format(VulkanRenderPass.formatSimpleReadImage);
			viewCreate.components().set(
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY
			);
			viewCreate.subresourceRange().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					0,
					baseAtlas.AtlasMipLevels,
					0,
					3
			);

			vkResult = vkCreateImageView(device,viewCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create atlas view",vkResult);
			IMAGE_VIEW_BLOCK_ATLAS = pReturn.get(0);

			VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.mallocStack(1,stack);
			imageInfo.sampler(VK_NULL_HANDLE);
			imageInfo.imageView(IMAGE_VIEW_BLOCK_ATLAS);
			imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

			VkWriteDescriptorSet.Buffer pDescriptorWrite = VkWriteDescriptorSet.mallocStack(1,stack);
			pDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
			pDescriptorWrite.pNext(VK_NULL_HANDLE);
			pDescriptorWrite.dstSet(DESCRIPTOR_SET_ATLAS);
			pDescriptorWrite.dstBinding(0);
			pDescriptorWrite.dstArrayElement(0);
			pDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			pDescriptorWrite.pImageInfo(imageInfo);

			vkUpdateDescriptorSets(device,pDescriptorWrite,null);

			int mip_offset = 0;
			int atlas_size = baseAtlas.AtlasWidth;
			for (int mip = 0; mip < baseAtlas.AtlasMipLevels; mip++) {
				baseAtlas.DataDiff.position(mip_offset * 4);
				baseAtlas.DataNorm.position(mip_offset * 4);
				baseAtlas.DataPBR.position(mip_offset * 4);

				command.SingleUseImagePopulate(
						IMAGE_BLOCK_ATLAS_ARRAY,baseAtlas.DataDiff,
						atlas_size,atlas_size,0,mip
				);
				command.SingleUseImagePopulate(
						IMAGE_BLOCK_ATLAS_ARRAY,baseAtlas.DataNorm,
						atlas_size,atlas_size,1,mip
				);
				command.SingleUseImagePopulate(
						IMAGE_BLOCK_ATLAS_ARRAY,baseAtlas.DataPBR,
						atlas_size,atlas_size,2,mip
				);

				//Next...
				mip_offset += atlas_size * atlas_size;
				atlas_size /= 2;
			}
			baseAtlas.DataDiff.position(0);
			baseAtlas.DataNorm.position(0);
			baseAtlas.DataPBR.position(0);
		}
	}

	private long CreateDescriptorPool(VkDevice device,MemoryStack old_stack) {
		try(MemoryStack stack = old_stack.push()) {
			VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.mallocStack(1,stack);
			poolSizes.position(0);
			poolSizes.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			poolSizes.descriptorCount(64+64+64+1);

			VkDescriptorPoolCreateInfo createPool = VkDescriptorPoolCreateInfo.mallocStack(stack);
			createPool.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
			createPool.pNext(VK_NULL_HANDLE);
			createPool.flags(0);
			createPool.maxSets(1);
			createPool.pPoolSizes(poolSizes);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateDescriptorPool(device,createPool,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create descriptor pool",vkResult);
			return pReturn.get(0);
		}
	}

	private long CreateDescriptorSet(VkDevice device,MemoryStack old_stack) {
		try(MemoryStack stack = old_stack.push()) {
			VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.mallocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.descriptorPool(DESCRIPTOR_POOL_ATLAS);
			allocateInfo.pSetLayouts(stack.longs(DESCRIPTOR_SET_LAYOUT_WORLD_ATLAS));

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkAllocateDescriptorSets(device,allocateInfo,pReturn);
			VulkanUtility.ValidateSuccess("Failed to allocate texture descriptor set",vkResult);
			return pReturn.get(0);
		}
	}

	private VulkanShaderModule CreateShader(String id,String path) {
		return new VulkanShaderModule(id,ResourceManager.getResource(ResourceType.SHADER,path));
	}

	@SuppressWarnings("Duplicates")
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
				pushConstants.size(3 * 4);
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
				//World Atlas Bindings
				/*
				VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.mallocStack(1, stack);
				bindings.position(0);
				{
					bindings.binding(0);
					bindings.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
					bindings.descriptorCount(1);
					bindings.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);
					bindings.pImmutableSamplers(null);//stack.longs(SAMPLER_BLOCK_ATLAS)); //TODO: CHANGE BACK
				}*/
				VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.mallocStack(4, stack);
				for(int i = 0; i < 3; i++) {
					bindings.position(i);
					bindings.binding(i);
					bindings.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE);
					bindings.descriptorCount(64);
					bindings.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);
					bindings.pImmutableSamplers(null);
				}
				bindings.position(3);
				{
					bindings.binding(3);
					bindings.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER);
					bindings.descriptorCount(1);
					bindings.stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS);
					bindings.pImmutableSamplers(null);
				}
				bindings.position(0);
				createInfo.pBindings(bindings);
			}else if(layoutID == 2) {
				VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.mallocStack(1, stack);
				bindings.position(0);
				{
					bindings.binding(0);
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

	@Deprecated
	private long CreateImmutableSampler(VulkanDevice device,MemoryStack old_stack,int mip_levels,float anisotropy) {
		try(MemoryStack stack = old_stack.push()) {
			VkSamplerCreateInfo imageSampler = VkSamplerCreateInfo.mallocStack(stack);
			imageSampler.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
			imageSampler.pNext(VK_NULL_HANDLE);
			imageSampler.flags(0);
			imageSampler.magFilter(VK_FILTER_NEAREST);//TODO: CHOOSE PROPERLY
			imageSampler.minFilter(VK_FILTER_NEAREST);
			imageSampler.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);//TODO: NEAREST REQUIRED??
			imageSampler.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			imageSampler.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			imageSampler.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
			imageSampler.mipLodBias(0.0f);
			if(device.features.samplerAnisotropy() && anisotropy > 0.0f) {
				float deviceAnisotropy = device.properties.limits().maxSamplerAnisotropy();
				float chosenAnisotropy = MathUtilities.clamp(deviceAnisotropy,1.0f,anisotropy);
				VulkanUtility.LogInfo("Block Atlas Anisotropy: Enabled ["+chosenAnisotropy+"]");
				imageSampler.anisotropyEnable(true);
				imageSampler.maxAnisotropy(chosenAnisotropy);
			}else {
				VulkanUtility.LogInfo("Block Atlas Anisotropy: Unavailable");
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
