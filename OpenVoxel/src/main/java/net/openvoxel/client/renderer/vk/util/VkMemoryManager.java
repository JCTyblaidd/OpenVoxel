package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.vk.VkGUIRenderer;
import net.openvoxel.client.renderer.vk.VkRenderer;
import net.openvoxel.client.renderer.vk.VkStats;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

//TODO: handle alignment for all allocations applicable
public class VkMemoryManager {

	public VkDeviceState state;
	private static final int MAX_ALLOCATIONS = 512;
	private LongBuffer allocationInfo;
	private static final boolean trackMemory = OpenVoxel.getLaunchParameters().hasFlag("vkTrackMemory");

	public LongBuffer memGuiStaging;
	public LongBuffer memGuiDrawing;

	public long guiImageMemory = VK_NULL_HANDLE;
	private ByteBuffer blockState;

	//Used by world renderer//
	//public LongBuffer memChunks;


	VkMemoryManager(VkDeviceState state) {
		this.state = state;
		if(trackMemory) {
			allocationInfo = MemoryUtil.memAllocLong(MAX_ALLOCATIONS * 2);
		}
	}

	public void cleanup() {
		if(trackMemory) {
			MemoryUtil.memFree(allocationInfo);
		}
	}

	void initStandardMemory() {
		try(MemoryStack stack = stackPush()) {
			memGuiStaging = MemoryUtil.memAllocLong(2);
			memGuiDrawing = MemoryUtil.memAllocLong(2);
			blockState = MemoryUtil.memCalloc(VkGUIRenderer.GUI_IMAGE_BLOCK_COUNT);
			int gui_usage_mask = VkGUIRenderer.GUI_USE_COHERENT_MEMORY ? VK_MEMORY_PROPERTY_HOST_COHERENT_BIT : 0;
			AllocateExclusive(VkGUIRenderer.GUI_BUFFER_SIZE + VkGUIRenderer.GUI_IMAGE_CACHE_SIZE,
					VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT | gui_usage_mask,
					memGuiStaging,stack);
			AllocateExclusive(VkGUIRenderer.GUI_BUFFER_SIZE,
					VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
					VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
					memGuiDrawing,stack);
		}
	}


	void clearStandardMemory() {
		FreeExclusive(memGuiStaging);
		FreeExclusive(memGuiDrawing);
		MemoryUtil.memFree(memGuiStaging);
		MemoryUtil.memFree(memGuiDrawing);
		MemoryUtil.memFree(blockState);
		if(guiImageMemory != VK_NULL_HANDLE) {
			VkStats.FreeMemory(state.renderDevice.device, guiImageMemory, null);
		}
	}

	void recreateStandardMemory() {
		//NO OP//
	}

	/////////////////////////
	/// Utility Functions ///
	/////////////////////////

	public void FreeGuiImage(long image, int offset, int count) {
		for(int i = 0; i < count; i++) {
			blockState.put(offset+i,(byte)0);
		}
		vkDestroyImage(state.renderDevice.device,image,null);
	}

	//Image -> Offset -> Count
	public void AllocateGuiImage(int format,int width, int height,int usage,int memoryProperties,LongBuffer returnValue,MemoryStack stack,boolean shared) {
		VkImageCreateInfo createInfo = VkImageCreateInfo.mallocStack(stack);
		VkExtent3D extent3D = VkExtent3D.mallocStack(stack);
		extent3D.set(width,height,1);
		createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
		createInfo.pNext(VK_NULL_HANDLE);
		createInfo.flags(0);
		createInfo.imageType(VK_IMAGE_TYPE_2D);
		createInfo.format(format);
		createInfo.extent(extent3D);
		createInfo.mipLevels(1);
		createInfo.arrayLayers(1);
		createInfo.samples(VK_SAMPLE_COUNT_1_BIT);
		createInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
		createInfo.usage(usage);
		if(shared && state.renderDevice.asyncTransfer) {
			createInfo.sharingMode(VK_SHARING_MODE_CONCURRENT);
			createInfo.pQueueFamilyIndices(stack.ints(state.renderDevice.queueFamilyIndexRender,state.renderDevice.queueFamilyIndexTransfer));
		}else {
			createInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			createInfo.pQueueFamilyIndices(null);
		}
		createInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

		int originalPosition = returnValue.position();

		if(vkCreateImage(state.renderDevice.device,createInfo,null,returnValue) != VK_SUCCESS) {
			throw new RuntimeException("Failed to create image");
		}
		long ReturnImage = returnValue.get(originalPosition);

		VkMemoryRequirements memoryRequirements = VkMemoryRequirements.mallocStack(stack);
		vkGetImageMemoryRequirements(state.renderDevice.device,ReturnImage,memoryRequirements);

		if(guiImageMemory == VK_NULL_HANDLE) {
			int memoryIndex = state.renderDevice.findMemoryType(memoryRequirements.memoryTypeBits(),memoryProperties);
			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.mallocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.allocationSize(VkGUIRenderer.GUI_IMAGE_CACHE_SIZE);
			allocateInfo.memoryTypeIndex(memoryIndex);
			LongBuffer returnBuf = stack.longs(0);
			if(VkStats.AllocMemory(state.renderDevice.device,allocateInfo,null,returnBuf) != VK_SUCCESS) {
				VkRenderer.Vkrenderer.getWorldRenderer().shrinkMemory(VkGUIRenderer.GUI_IMAGE_CACHE_SIZE);
				if(VkStats.AllocMemory(state.renderDevice.device, allocateInfo, null, returnBuf) != VK_SUCCESS) {
					vkDestroyImage(state.renderDevice.device, ReturnImage, null);
					throw new RuntimeException("Failed to allocate Image");
				}
				VkRenderer.Vkrenderer.getWorldRenderer().growMemory();
			}
			guiImageMemory = returnBuf.get(0);
		}

		int blockCount = (int)(memoryRequirements.size() + VkGUIRenderer.GUI_IMAGE_BLOCK_SIZE - 1) / VkGUIRenderer.GUI_IMAGE_BLOCK_SIZE;

		//Find Valid Allocation Blocks//
		int blockOffset = 0;
		int validCount = 0;
		for(int block = 0; block < VkGUIRenderer.GUI_IMAGE_BLOCK_COUNT; block++) {
			if(blockState.get(block) == 0) {
				validCount++;
				if(validCount == blockCount) {
					break;
				}
			}else{
				validCount = 0;
				blockOffset = block+1;
			}
		}
		if(blockOffset == VkGUIRenderer.GUI_IMAGE_BLOCK_COUNT || validCount != blockCount) {
			throw new RuntimeException("Failed to find image binding location");
		}

		//Bind//
		if(vkBindImageMemory(state.renderDevice.device,ReturnImage,guiImageMemory,blockOffset * VkGUIRenderer.GUI_IMAGE_BLOCK_SIZE) != VK_SUCCESS) {
			throw new RuntimeException("Failed to bind image memory");
		}
		for(int i = 0; i < blockCount; i++) {
			blockState.put(blockOffset + i,(byte)1);
		}

		//Add Unblock Memory Info//
		returnValue.put(originalPosition+1,blockOffset);
		returnValue.put(originalPosition+2,blockCount);
	}

	/**
	 *
	 * @param size the number of bytes to allocate
	 * @param usage the usage of the buffer
	 * @param memoryProperties the properties required
	 * @param returnValue long buffer --> buffer then memory
	 * @param stack memory stack to use
	 *              //TODO: alignment returned -> use how?
	 */
	public void AllocateExclusive(int size,int usage,int memoryProperties,LongBuffer returnValue,MemoryStack stack) {
		VkBufferCreateInfo createInfo = VkBufferCreateInfo.mallocStack(stack);
		createInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
		createInfo.pNext(VK_NULL_HANDLE);
		createInfo.flags(0);
		createInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
		createInfo.pQueueFamilyIndices(null);
		createInfo.usage(usage);
		createInfo.size(size);
		if(vkCreateBuffer(state.renderDevice.device,createInfo,null,returnValue) != VK_SUCCESS) {
			throw new RuntimeException("Failed to create exclusive buffer");
		}
		int oldPosition = returnValue.position();
		int newPosition = oldPosition+1;
		returnValue.position(newPosition);
		VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
		vkGetBufferMemoryRequirements(state.renderDevice.device,returnValue.get(oldPosition),requirements);
		int memoryIndex = state.renderDevice.findMemoryType(requirements.memoryTypeBits(),memoryProperties);
		VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.callocStack(stack);
		allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
		allocateInfo.pNext(VK_NULL_HANDLE);
		allocateInfo.allocationSize(requirements.size());
		allocateInfo.memoryTypeIndex(memoryIndex);
		if(VkStats.AllocMemory(state.renderDevice.device,allocateInfo,null,returnValue) != VK_SUCCESS) {
			VkRenderer.Vkrenderer.getWorldRenderer().shrinkMemory(requirements.size());
			if(VkStats.AllocMemory(state.renderDevice.device, allocateInfo, null, returnValue) != VK_SUCCESS) {
				vkDestroyBuffer(state.renderDevice.device,returnValue.get(oldPosition),null);
				throw new RuntimeException("Failed to create buffer backing memory");
			}
			VkRenderer.Vkrenderer.getWorldRenderer().growMemory();
		}
		if(vkBindBufferMemory(state.renderDevice.device,returnValue.get(oldPosition),returnValue.get(newPosition),0) != VK_SUCCESS) {
			vkDestroyBuffer(state.renderDevice.device,returnValue.get(oldPosition),null);
			VkStats.FreeMemory(state.renderDevice.device, returnValue.get(newPosition), null);
			throw new RuntimeException("Failed to bind buffer memory");
		}
		returnValue.position(oldPosition);
	}

	public void FreeExclusive(LongBuffer value) {
		vkDestroyBuffer(state.renderDevice.device,value.get(0),null);
		VkStats.FreeMemory(state.renderDevice.device,value.get(1),null);
	}

	public ByteBuffer mapMemory(long memoryHandle,int offset, int size,MemoryStack stack) {
		PointerBuffer pBuffer = stack.mallocPointer(1);
		if(vkMapMemory(state.renderDevice.device,memoryHandle,offset,size,0,pBuffer) != VK_SUCCESS) {
			throw new RuntimeException("Failed to map memory");
		}
		return pBuffer.getByteBuffer(0,size);
	}

	public void unMapMemory(long memoryHandle) {
		vkUnmapMemory(state.renderDevice.device,memoryHandle);
	}

}
