package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.vk.VkRenderer;
import net.openvoxel.client.renderer.vk.VkStats;
import net.openvoxel.client.renderer.vk.VkTexAtlas;
import net.openvoxel.client.renderer.vk.util.VkRenderManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRMaintenance1.VK_FORMAT_FEATURE_TRANSFER_DST_BIT_KHR;
import static org.lwjgl.vulkan.KHRMaintenance1.VK_FORMAT_FEATURE_TRANSFER_SRC_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VkWorldRenderManager {

	private VkRenderManager renderManager;

	//Render Pipelines//

	//Descriptor Set//
	private long descriptor_pool;
	private LongBuffer descriptor_sets;

	//Texture Atlas Binding//
	private long ImageAtlas = 0;
	private long ImageAtlasMemory;
	private long ImageViewDiffuse;
	private long ImageViewNormal;
	private long ImageViewPBR;

	//Image Formats//
	private int imageDepthFormat;
	private int HDRColorFormat;
	private int HDRColorAlphaFormat;

	/////////////////
	//Target Images//
	/////////////////
	/*
		CubeMap 6x Images of the environment
	 */
	private long ImageEnvCubeMap;
	/*
		GBuffer Target:
		0 - Depth           [depth]
		1 - Diffuse         [rgb]
		2 - Normal          [rgb]
		3 - PBR             [rgb]
		4 - Lighting/Merge  [rgb]
		5 - Lighting Post   [rgb]
	 */
	private long ImageGBuffer;
	private long ImageShadowMap;

	private void create_texture_atlas(VkTexAtlas atlas) {
		try(MemoryStack stack = stackPush()) {
			VkExtent3D extent = VkExtent3D.mallocStack(stack);
			extent.set(atlas.pack_width,atlas.pack_height,1);
			VkImageCreateInfo createInfo = VkImageCreateInfo.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			createInfo.pNext(VK_NULL_HANDLE);
			createInfo.flags(0);
			createInfo.imageType(VK_IMAGE_TYPE_2D);
			createInfo.format(VK_FORMAT_R8G8B8A8_UNORM);
			createInfo.extent(extent);
			createInfo.mipLevels(atlas.pack_mip_count);
			createInfo.arrayLayers(3);
			createInfo.samples(VK_SAMPLE_COUNT_1_BIT);
			createInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
			createInfo.usage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
			createInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			createInfo.pQueueFamilyIndices(null);
			createInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			LongBuffer res = stack.mallocLong(1);
			if(vkCreateImage(renderManager.renderDevice.device,createInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create texture atlas");
			}
			ImageAtlas = res.get(0);

			VkMemoryRequirements memoryReqs = VkMemoryRequirements.mallocStack(stack);
			vkGetImageMemoryRequirements(renderManager.renderDevice.device,ImageAtlas,memoryReqs);
			int memoryIndex = renderManager.renderDevice.findMemoryType(memoryReqs.memoryTypeBits(),
					VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.callocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.allocationSize(memoryReqs.size());
			allocateInfo.memoryTypeIndex(memoryIndex);
			if(VkStats.AllocMemory(renderManager.renderDevice.device,allocateInfo,null,res) != VK_SUCCESS) {
				VkRenderer.Vkrenderer.getWorldRenderer().shrinkMemory((int)memoryReqs.size());
				if(VkStats.AllocMemory(renderManager.renderDevice.device,allocateInfo,null,res) != VK_SUCCESS) {
					throw new RuntimeException("Failed to allocate device memory");
				}
				VkRenderer.Vkrenderer.getWorldRenderer().growMemory();
			}
			ImageAtlasMemory = res.get(0);
			if(vkBindImageMemory(renderManager.renderDevice.device,ImageAtlas,ImageAtlasMemory,0) != VK_SUCCESS) {
				throw new RuntimeException("Failed to bind image memory");
			}

			VkComponentMapping componentMapping = VkComponentMapping.mallocStack(stack);
			componentMapping.r(VK_COMPONENT_SWIZZLE_IDENTITY);
			componentMapping.g(VK_COMPONENT_SWIZZLE_IDENTITY);
			componentMapping.b(VK_COMPONENT_SWIZZLE_IDENTITY);
			componentMapping.a(VK_COMPONENT_SWIZZLE_IDENTITY);

			VkImageSubresourceRange range = VkImageSubresourceRange.mallocStack(stack);
			range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			range.baseMipLevel(0);
			range.levelCount(atlas.pack_mip_count);
			range.baseArrayLayer(0);
			range.layerCount(1);

			VkImageViewCreateInfo viewCreateInfo = VkImageViewCreateInfo.mallocStack(stack);
			viewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			viewCreateInfo.pNext(VK_NULL_HANDLE);
			viewCreateInfo.flags(0);
			viewCreateInfo.image(ImageAtlas);
			viewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
			viewCreateInfo.format(VK_FORMAT_R8G8B8A8_UNORM);
			viewCreateInfo.components(componentMapping);
			viewCreateInfo.subresourceRange(range);

			if(vkCreateImageView(renderManager.renderDevice.device,viewCreateInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create image view");
			}
			ImageViewDiffuse = res.get(0);

			range.baseArrayLayer(1);
			viewCreateInfo.subresourceRange(range);
			if(vkCreateImageView(renderManager.renderDevice.device,viewCreateInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create image view");
			}
			ImageViewNormal = res.get(0);

			range.baseArrayLayer(2);
			viewCreateInfo.subresourceRange(range);
			if(vkCreateImageView(renderManager.renderDevice.device,viewCreateInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create image view");
			}
			ImageViewPBR = res.get(0);
		}
		update_texture_atlas_memory(atlas);
	}

	private void destroy_texture_atlas() {
		if(ImageAtlas != 0) {
			vkDestroyImageView(renderManager.renderDevice.device, ImageViewDiffuse, null);
			vkDestroyImageView(renderManager.renderDevice.device, ImageViewNormal, null);
			vkDestroyImageView(renderManager.renderDevice.device, ImageViewPBR, null);
			vkDestroyImage(renderManager.renderDevice.device, ImageAtlas, null);
			VkStats.FreeMemory(renderManager.renderDevice.device, ImageAtlasMemory, null);
			ImageAtlas = 0;
		}
	}

	private void update_texture_atlas_memory(VkTexAtlas atlas) {
		try(MemoryStack stack = stackPush()) {
			VkCommandBuffer cmd = renderManager.beginSingleUseCommand(stack);

			LongBuffer staging_buffer = stack.mallocLong(2);
			renderManager.memoryMgr.AllocateExclusive(atlas.pack_diffuse.capacity()*4,VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,staging_buffer,stack);

			VkExtent3D extent = VkExtent3D.mallocStack(stack);
			extent.width(atlas.pack_width);
			extent.height(atlas.pack_height);
			extent.depth(1);

			VkOffset3D offset = VkOffset3D.mallocStack(stack);
			offset.x(0);
			offset.y(0);
			offset.z(0);

			VkImageSubresourceLayers sub_resource = VkImageSubresourceLayers.mallocStack(stack);
			sub_resource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			sub_resource.mipLevel(0);
			sub_resource.baseArrayLayer(0);
			sub_resource.layerCount(3);

			VkBufferImageCopy.Buffer regions = VkBufferImageCopy.mallocStack(atlas.pack_mip_count,stack);
			for(int mip_target = 0; mip_target < atlas.pack_mip_count; mip_target++) {
				int mip_x_offset = mip_target == 0 ? 0 : atlas.pack_width;
				int mip_y_offset = 0;
				for(int mip_iterate = 2; mip_iterate <= mip_target; mip_iterate++) {
					int offset_sf = (1 << (mip_iterate - 1));
					mip_y_offset += (atlas.pack_height + offset_sf - 1) / offset_sf;
				}
				regions.position(mip_target);
				regions.bufferOffset(mip_x_offset + (mip_y_offset*atlas.expanded_pack_width));
				regions.bufferRowLength(atlas.expanded_pack_width);
				regions.bufferImageHeight(atlas.pack_height);
				regions.imageSubresource(sub_resource);
				regions.imageOffset(offset);
				regions.imageExtent(extent);
			}
			regions.position(0);

			VkImageSubresourceRange sub_resource_range = VkImageSubresourceRange.mallocStack(stack);
			sub_resource_range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			sub_resource_range.baseMipLevel(0);
			sub_resource_range.levelCount(atlas.pack_mip_count);
			sub_resource_range.baseArrayLayer(0);
			sub_resource_range.layerCount(3);

			VkImageMemoryBarrier.Buffer imgBarrier = VkImageMemoryBarrier.mallocStack(1,stack);
			imgBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
			imgBarrier.pNext(VK_NULL_HANDLE);
			imgBarrier.srcAccessMask(0);
			imgBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imgBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			imgBarrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			imgBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			imgBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			imgBarrier.image(ImageAtlas);
			imgBarrier.subresourceRange(sub_resource_range);

			vkCmdPipelineBarrier(cmd,VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT,VK_PIPELINE_STAGE_TRANSFER_BIT,0,
			null,null,imgBarrier);

			ByteBuffer mapped_memory = renderManager.memoryMgr.mapMemory(staging_buffer.get(1),0,
					atlas.pack_diffuse.capacity()*4,stack);
			mapped_memory.position(0);
			mapped_memory.put(atlas.pack_diffuse);
			mapped_memory.put(atlas.pack_normal);
			mapped_memory.put(atlas.pack_pbr);
			mapped_memory.position(0);
			renderManager.memoryMgr.unMapMemory(staging_buffer.get(1));

			vkCmdCopyBufferToImage(cmd,staging_buffer.get(0),ImageAtlas,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,regions);

			imgBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imgBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
			imgBarrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			imgBarrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			vkCmdPipelineBarrier(cmd,VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT,0,
					null,null,imgBarrier);

			renderManager.endSingleUseCommand(stack,cmd);
			renderManager.memoryMgr.FreeExclusive(staging_buffer);
		}
	}

	public VkWorldRenderManager(VkRenderManager manager) {
		renderManager = manager;
	}
	
	private boolean has_bit(int src,int bit) {
		return (src & bit) == bit;
	}

	private void debugFormat(String id,int format,boolean optimal,int features) {
		List<String> feature_list = new ArrayList<>();
		if(optimal) {
			feature_list.add("OPTIMAL_FORMAT");
		}else{
			feature_list.add("LINEAR_FORMAT");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT)) {
			feature_list.add("SAMPLED_IMAGE");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT)) {
			feature_list.add("STORAGE_IMAGE");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT)) {
			feature_list.add("STORAGE_IMAGE_ATOMIC");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT)) {
			feature_list.add("UNIFORM_TEXEL_BUFFER");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT)) {
			feature_list.add("STORAGE_TEXEL_BUFFER");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT)) {
			feature_list.add("STORAGE_TEXEL_BUFFER_ATOMIC");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)) {
			feature_list.add("VERTEX_BUFFER");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT)) {
			feature_list.add("COLOR_ATTACHMENT");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT)) {
			feature_list.add("COLOR_ATTACHMENT_BLEND");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT)) {
			feature_list.add("DEPTH_STENCIL_ATTACHMENT");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_BLIT_SRC_BIT)) {
			feature_list.add("BLIT_SRC");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_BLIT_DST_BIT)) {
			feature_list.add("BLIT_DST");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT)) {
			feature_list.add("SAMPLED_IMAGE_FILTER_LINEAR");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_TRANSFER_SRC_BIT_KHR)) {
			feature_list.add("TRANSFER_SRC_KHR");
		}
		if(has_bit(features,VK_FORMAT_FEATURE_TRANSFER_DST_BIT_KHR)) {
			feature_list.add("TRANSFER_DST_KHR");
		}
		Logger.getLogger("Vulkan").Debug(id,format,": ",String.join(",",feature_list));
	}

	private int findSupportedFormat(String debugID,MemoryStack stack, IntBuffer candidateFormats, int imageTiling, int formatFeatureFlags) {
		VkFormatProperties formatProps = VkFormatProperties.mallocStack(stack);
		for(int pos = 0; pos < candidateFormats.capacity(); pos++) {
			int format = candidateFormats.get(pos);
			vkGetPhysicalDeviceFormatProperties(renderManager.renderDevice.physicalDevice,format,formatProps);
			if(imageTiling == VK_IMAGE_TILING_LINEAR && has_bit(formatProps.linearTilingFeatures(),formatFeatureFlags)) {
				debugFormat(debugID,format,false,formatProps.linearTilingFeatures());
				return format;
			}else if(imageTiling == VK_IMAGE_TILING_OPTIMAL && has_bit(formatProps.optimalTilingFeatures(),formatFeatureFlags)) {
				debugFormat(debugID,format,true,formatProps.optimalTilingFeatures());
				return format;
			}
		}
		throw new RuntimeException("Failed to find supported image format");
	}

	public void initDeviceMetaInfo() {
		try(MemoryStack stack = stackPush()) {
			imageDepthFormat = findSupportedFormat("Depth Format: ",stack,
					stack.ints(VK_FORMAT_D32_SFLOAT,VK_FORMAT_D32_SFLOAT_S8_UINT,VK_FORMAT_D24_UNORM_S8_UINT),
					VK_IMAGE_TILING_OPTIMAL,
					VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT |
							VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT |
							VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT);
			HDRColorFormat = findSupportedFormat("HDR Format:",stack,
					stack.ints(VK_FORMAT_R32G32B32_SFLOAT,VK_FORMAT_R16G16B16_SFLOAT,VK_FORMAT_R32G32B32A32_SFLOAT),
					VK_IMAGE_TILING_OPTIMAL,
					VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT |
							VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT);
			HDRColorAlphaFormat = findSupportedFormat("HDR-Alpha:",stack,
					stack.ints(VK_FORMAT_R32G32B32A32_SFLOAT,VK_FORMAT_R16G16B16A16_SFLOAT),
					VK_IMAGE_TILING_OPTIMAL,
					VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT |
							VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT);
		}
	}

	/**
	 * Initialize Resource Texture [Called on resource load/update]
	 */
	public void initTextures() {
		create_texture_atlas(VkRenderer.Vkrenderer.getBlockAtlas());
	}

	/**
	 * Destroy Resource Textures [Called on resource unload/update]
	 */
	public void destroyTextures() {
		destroy_texture_atlas();
	}

	/**
	 * Create Descriptor Sets for world rendering
	 *
	 * Descriptors:
	 *      Texture Atlas:
	 *          Sampler x3: Diffuse, Normal, PBR {Texture Atlas}
	 *      Per Draw Constants:
	 *          Mat4 projMatrix;
	 *          Mat4 playerPosMatrix;
	 *
	 *  Push Constants:
	 *      Mat4 chunkPosMatrix
	 */
	public void createDescriptorSets() {
		descriptor_sets = MemoryUtil.memAllocLong(renderManager.swapChainImageViews.capacity());
		try(MemoryStack stack = stackPush()) {
			LongBuffer pRet = stack.mallocLong(1);

			VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.mallocStack(1,stack);
			poolSizes.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			poolSizes.descriptorCount(1);

			VkDescriptorPoolCreateInfo createPool = VkDescriptorPoolCreateInfo.mallocStack(stack);
			createPool.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
			createPool.pNext(VK_NULL_HANDLE);
			createPool.flags(0);
			createPool.maxSets(descriptor_sets.capacity());
			createPool.pPoolSizes(poolSizes);
			if(vkCreateDescriptorPool(renderManager.renderDevice.device,createPool,null,pRet) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create Descriptor Pool");
			}
			descriptor_pool = pRet.get(0);
		}
	}

	public void destroyDescriptorSets() {
		vkDestroyDescriptorPool(renderManager.renderDevice.device,descriptor_pool,null);
		MemoryUtil.memFree(descriptor_sets);
	}

	public void initRenderTargets() {

	}

	public void destroyRenderTargets() {

	}

	public void initFrameBuffers() {

	}

	public void destroyFrameBuffers() {

	}

	public void initPipelines(List<String> defines) {

	}

	public void destroyPipelines() {

	}
}
