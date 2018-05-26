package net.openvoxel.client.renderer.vk.renderpass;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents an image resource used during
 *  the render pass builder
 */
public class PassResource {

	public static final int VIEWPORT_SIZE = -1;

	private long image;
	private long image_view;
	private long image_memory;

	private int width;
	private int height;
	private int depth;
	private boolean viewport_locked;
	private int layers;
	final int format;
	final int samples;
	private boolean localUsage;

	private TIntIntMap passImageLayoutMap;      //pass-index => required layout
	private TIntIntMap passKeepPreviousMap;     //pass-index => load previous pass data
	private TIntIntMap passClearAtMap;          //pass-index => call clear

	boolean is_same_dimensions(PassEntry.OutputAttachment output) {
		if(width != output.width) return false;
		if(height != output.height) return false;
		if(depth != output.depth) return false;
		if(layers != output.layers) return false;
		if(format != output.format) return false;
		return samples == output.samples;
	}

	boolean is_depth_format() {
		if(format == VK_FORMAT_D32_SFLOAT) return true;
		if(format == VK_FORMAT_D32_SFLOAT_S8_UINT) return true;
		if(format == VK_FORMAT_D24_UNORM_S8_UINT) return true;
		if(format == VK_FORMAT_D16_UNORM) return true;
		if(format == VK_FORMAT_D16_UNORM_S8_UINT) return true;
		return format == VK_FORMAT_X8_D24_UNORM_PACK32;
	}

	private int get_usage() {
		int usage = VK_IMAGE_USAGE_SAMPLED_BIT;
		if(is_depth_format()) {
			usage |= VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
		}
		if(depth == 1) {
			usage |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
			if(localUsage) {
				usage |= VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT;
			}
		}else{
			usage |= VK_IMAGE_USAGE_STORAGE_BIT;
		}
		return usage;
	}

	public boolean dependsOnViewport() {
		return viewport_locked;
	}

	void resetRequires() {
		passKeepPreviousMap.clear();
		passImageLayoutMap.clear();
		passClearAtMap.clear();
	}

	void markClearAt(int passID) {
		passClearAtMap.put(passID,1);
	}

	void setRequirePrevious(int passID, boolean require) {
		passKeepPreviousMap.put(passID,require ? 1 : 0);
	}

	void setRequiredLayout(int passID, int imageLayout) {
		passImageLayoutMap.put(passID,imageLayout);
	}

	void calculateLayouts(int maxPassID) {
		int currentLayout = VK_IMAGE_LAYOUT_UNDEFINED;
		int currentRequirePrevious = 0;
		int currentClearAt = 0;
		for(int i = maxPassID; i >= 0; i--) {
			int getLayout = passImageLayoutMap.get(i);
			if(getLayout == passImageLayoutMap.getNoEntryValue()) {
				passImageLayoutMap.put(i,currentLayout);
			}else{
				currentLayout = getLayout;
			}
			int keepPrevious = passKeepPreviousMap.get(i);
			if(keepPrevious == passKeepPreviousMap.getNoEntryValue()) {
				passKeepPreviousMap.put(i,currentRequirePrevious);
				keepPrevious = currentRequirePrevious;
			}else{
				currentRequirePrevious = keepPrevious;
			}
			boolean clearAt = passClearAtMap.containsKey(i);
			if(!clearAt) {
				//If keep previous -> stop propagation
				if(keepPrevious != 0) {
					passClearAtMap.put(i,0);
					currentClearAt = 0;
				}else{
					passClearAtMap.put(i,currentClearAt);
				}
			}else{
				currentClearAt = 1;
			}
		}
	}

	int getLayout(int passID) {
		return passImageLayoutMap.get(passID);
	}

	boolean requirePrevious(int passID) {
		return passKeepPreviousMap.get(passID) != 0;
	}

	boolean clearAt(int passID) {
		return passClearAtMap.get(passID) != 0;
	}

	public PassResource(int width, int height, int depth, int layers, int format, int samples) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.viewport_locked = width == VIEWPORT_SIZE || height == VIEWPORT_SIZE || depth == VIEWPORT_SIZE;
		this.layers = layers;
		this.format = format;
		this.samples = samples;
		this.localUsage = false;
		passImageLayoutMap = new TIntIntHashMap();
		passKeepPreviousMap = new TIntIntHashMap();
		passClearAtMap = new TIntIntHashMap();
		image = VK_NULL_HANDLE;
		image_view = VK_NULL_HANDLE;
		image_memory = VK_NULL_HANDLE;
	}

	public void markUsesLocal() {
		localUsage = true;
	}

	private int get_dimension(int size, int fallback_size) {
		return size == VIEWPORT_SIZE ? fallback_size : size;
	}

	public void load(VkDevice device,VulkanMemory memory, int screenWidth, int screenHeight) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkImageCreateInfo imageCreate = VkImageCreateInfo.mallocStack(stack);
			imageCreate.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageCreate.flags(0);
			imageCreate.imageType(depth == 1 ? VK_IMAGE_TYPE_2D : VK_IMAGE_TYPE_3D);
			imageCreate.format(format);
			imageCreate.extent().set(
					get_dimension(width,screenWidth),
					get_dimension(height,screenHeight),
					get_dimension(depth,1)
			);
			imageCreate.mipLevels(1);
			imageCreate.arrayLayers(layers);
			imageCreate.samples(samples);
			imageCreate.tiling(VK_IMAGE_TILING_OPTIMAL);
			imageCreate.usage(get_usage());
			imageCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			imageCreate.pQueueFamilyIndices(null);
			imageCreate.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateImage(device,imageCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to create pass resource",vkResult);
			image = pReturn.get(0);

			image_memory = memory.allocateDedicatedImage(image,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			vkResult = vkBindImageMemory(device,image,image_memory,0L);
			VulkanUtility.ValidateSuccess("Failed to bind pass memory",vkResult);

			VkImageViewCreateInfo viewCreate = VkImageViewCreateInfo.mallocStack(stack);
			viewCreate.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			viewCreate.pNext(VK_NULL_HANDLE);
			viewCreate.flags(0);
			viewCreate.image(image);
			viewCreate.viewType(depth == 1 ? VK_IMAGE_VIEW_TYPE_2D : VK_IMAGE_VIEW_TYPE_3D);
			viewCreate.format(format);
			viewCreate.components().set(
				VK_COMPONENT_SWIZZLE_IDENTITY,
				VK_COMPONENT_SWIZZLE_IDENTITY,
				VK_COMPONENT_SWIZZLE_IDENTITY,
				VK_COMPONENT_SWIZZLE_IDENTITY
			);
			viewCreate.subresourceRange().set(
				is_depth_format() ? VK_IMAGE_ASPECT_DEPTH_BIT : VK_IMAGE_ASPECT_COLOR_BIT,
				0,
				1,
				0,
				1
			);
			vkResult = vkCreateImageView(device,viewCreate,null,pReturn);
			VulkanUtility.ValidateSuccess("Failed to creat pass view",vkResult);
			image_view = pReturn.get(0);
		}
	}

	public boolean reload(VkDevice device, VulkanMemory memory, int width, int height) {
		if(viewport_locked) {
			close(device, memory);
			load(device,memory,width,height);
			return true;
		}else{
			return false;
		}
	}

	public void close(VkDevice device,VulkanMemory memory) {
		if(image != VK_NULL_HANDLE) {
			vkDestroyImageView(device,image_view,null);
			vkDestroyImage(device,image,null);
			memory.freeDedicatedMemory(image_memory);
			image = VK_NULL_HANDLE;
			image_view = VK_NULL_HANDLE;
			image_memory = VK_NULL_HANDLE;
		}
	}
}
