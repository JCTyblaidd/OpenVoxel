package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.vk.util.VkRenderManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFormatProperties;

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
	private int ImageAtlasDiffuse;
	private int ImageAtlasNormal;
	private int ImageAtlasPBR;
	private int ImageViewDiffuse;
	private int ImageViewNormal;
	private int ImageViewPBR;

	//Image Formats//
	private int imageDepthFormat;
	private int HDRColorFormat;
	private int HDRColorAlphaFormat;

	//GBuffer Binding: 0=image,1=memory,2=imageview//
	private LongBuffer ImageEnvCubeMap;
	private LongBuffer ImageGBuffer;
	private LongBuffer ImageShadowMap;


	private void create_image_target() {

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