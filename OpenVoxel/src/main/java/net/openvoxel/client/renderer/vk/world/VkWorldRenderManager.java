package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.vk.VkTexAtlas;
import net.openvoxel.client.renderer.vk.util.VkRenderManager;
import org.lwjgl.system.MemoryStack;
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


	//Texture Atlas Binding//
	private long ImageAtlas;
	private long ImageAtlasMemory;
	private long ImageViewDiffuse;
	private long ImageViewNormal;
	private long ImageViewPBR;

	//Image Formats//
	private int imageDepthFormat;
	private int HDRColorFormat;
	private int HDRColorAlphaFormat;

	//GBuffer Binding: 0=image,1=memory,2=imageview//
	private LongBuffer ImageEnvCubeMap;
	private LongBuffer ImageGBuffer;
	private LongBuffer ImageShadowMap;


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
			createInfo.usage(VK_IMAGE_USAGE_SAMPLED_BIT);
			createInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			createInfo.pQueueFamilyIndices(null);
			createInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			LongBuffer res = stack.mallocLong(1);
			if(vkCreateImage(renderManager.renderDevice.device,createInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create texture atlas");
			}
			ImageAtlas = res.get(0);

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

			VkMemoryRequirements memoryReqs = VkMemoryRequirements.mallocStack(stack);
			vkGetImageMemoryRequirements(renderManager.renderDevice.device,ImageAtlas,memoryReqs);
			int memoryIndex = renderManager.renderDevice.findMemoryType(memoryReqs.memoryTypeBits(),
					VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.callocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.allocationSize(memoryReqs.size());
			allocateInfo.memoryTypeIndex(memoryIndex);
			if(vkAllocateMemory(renderManager.renderDevice.device,allocateInfo,null,res) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate device memory");
			}
			ImageAtlasMemory = res.get(0);
			if(vkBindImageMemory(renderManager.renderDevice.device,ImageAtlas,ImageAtlasMemory,0) != VK_SUCCESS) {
				throw new RuntimeException("Failed to bind image memory");
			}
		}
	}

	private void destroy_texture_atlas() {
		vkDestroyImageView(renderManager.renderDevice.device,ImageViewDiffuse,null);
		vkDestroyImageView(renderManager.renderDevice.device,ImageViewNormal, null);
		vkDestroyImageView(renderManager.renderDevice.device,ImageViewPBR, null);
		vkDestroyImage(renderManager.renderDevice.device,ImageAtlas,null);
		vkFreeMemory(renderManager.renderDevice.device,ImageAtlasMemory,null);
	}

	private void update_texture_atlas_memory() {
		try(MemoryStack stack = stackPush()) {
			VkCommandBuffer cmd = renderManager.beginSingleUseCommand(stack);
			LongBuffer staging_buffer = renderManager.memoryMgr.memGuiStaging;
			VkBufferImageCopy.Buffer regions = VkBufferImageCopy.mallocStack(1,stack);
			regions.bufferOffset(0);
			regions.bufferRowLength(0);
			regions.bufferImageHeight(0);
			regions.imageSubresource();
			regions.imageOffset();
			regions.imageExtent();



			//vkCmdPipelineBarrier(cmd,)
			//vkCmdCopyBufferToImage(cmd,staging_buffer,ImageAtlas,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,regions);
			//vkCmdPipelineBarrier(cmd,);

			renderManager.endSingleUseCommand(stack,cmd);
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
