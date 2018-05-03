package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.STBITexture;
import net.openvoxel.client.renderer.base.BaseTextRenderer;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.client.renderer.vk.pipeline.VulkanRenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

class VulkanTextRenderer extends BaseTextRenderer {

	private long Image;
	long ImageView;
	private long ImageMemory;
	long ImageSampler;

	private VulkanCommandHandler commandHandler;
	private VulkanMemory memory;

	VulkanTextRenderer(String imgRes, VulkanCommandHandler commandHandler, VulkanMemory vulkanMemory) {
		super(imgRes);
		this.commandHandler = commandHandler;
		this.memory = vulkanMemory;
		load();
	}

	void load() {
		int format = VulkanRenderPass.formatSimpleReadImage;
		STBITexture texture = new STBITexture(handle.getByteData());
		try(MemoryStack stack = stackPush()) {
			VkImageCreateInfo imageCreate = VkImageCreateInfo.mallocStack(stack);
			imageCreate.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageCreate.pNext(VK_NULL_HANDLE);
			imageCreate.flags(0);
			imageCreate.imageType(VK_IMAGE_TYPE_2D);
			imageCreate.format(format);
			imageCreate.extent().set(texture.width,texture.height,1);
			imageCreate.mipLevels(1);
			imageCreate.arrayLayers(1);
			imageCreate.samples(VK_SAMPLE_COUNT_1_BIT);
			imageCreate.tiling(VK_IMAGE_TILING_OPTIMAL);
			imageCreate.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
			imageCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			imageCreate.pQueueFamilyIndices(null);
			imageCreate.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateImage(commandHandler.getDevice(),imageCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create text image",vkResult);
			Image = pReturn.get(0);

			ImageMemory = memory.allocateDedicatedImage(Image,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			if(ImageMemory == VK_NULL_HANDLE) {
				VulkanUtility.CrashOnBadResult("Failed to get memory for text image",-1);
			}

			vkResult = vkBindImageMemory(commandHandler.getDevice(),Image,ImageMemory,0);
			VulkanUtility.ValidateSuccess("Failed to bind image memory for text image",vkResult);

			VkImageViewCreateInfo imageViewCreate = VkImageViewCreateInfo.mallocStack(stack);
			imageViewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			imageViewCreate.pNext(VK_NULL_HANDLE);
			imageViewCreate.flags(0);
			imageViewCreate.image(Image);
			imageViewCreate.viewType(VK_IMAGE_VIEW_TYPE_2D);
			imageViewCreate.format(format);
			imageViewCreate.components().set(
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY,
					VK_COMPONENT_SWIZZLE_IDENTITY
			);
			imageViewCreate.subresourceRange().set(
					VK_IMAGE_ASPECT_COLOR_BIT,
					0,
					1,
					0,
					1
			);

			vkResult = vkCreateImageView(commandHandler.getDevice(),imageViewCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create Image View for Text",vkResult);
			ImageView = pReturn.get(0);

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
			imageSampler.anisotropyEnable(false);
			imageSampler.maxAnisotropy(1.0f);
			imageSampler.compareEnable(false);
			imageSampler.compareOp(VK_COMPARE_OP_ALWAYS);
			imageSampler.minLod(0.0f);
			imageSampler.maxLod(0.0f);
			imageSampler.borderColor(0);
			imageSampler.unnormalizedCoordinates(false);

			vkResult = vkCreateSampler(commandHandler.getDevice(),imageSampler,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create Image Sampler for Text",vkResult);
			ImageSampler = pReturn.get(0);

			//Push Memory to Image
			commandHandler.SingleUseImagePopulate(Image,texture);
		}finally {
			texture.Free();
		}
	}

	void close() {
		vkDestroySampler(commandHandler.getDevice(),ImageSampler,null);
		vkDestroyImageView(commandHandler.getDevice(),ImageView,null);
		vkDestroyImage(commandHandler.getDevice(),Image,null);
		memory.freeDedicatedMemory(ImageMemory);
	}

}
