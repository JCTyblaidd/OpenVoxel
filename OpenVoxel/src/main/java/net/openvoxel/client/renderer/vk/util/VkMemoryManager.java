package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkMemoryManager {

	public VkDeviceState state;
	private int MAX_ALLOCATIONS = 512;
	private LongBuffer allocationInfo;
	private static final boolean trackMemory = OpenVoxel.getLaunchParameters().hasFlag("vkTrackMemory");

	public LongBuffer memGuiStaging;
	public LongBuffer memGuiDrawing;

	//Used by world renderer//
	public LongBuffer memChunks;


	VkMemoryManager(VkDeviceState state) {
		this.state = state;
		if(trackMemory) {
		//	allocationInfo = MemoryUtil.memAllocLong(MAX_ALLOCATIONS * 2);
		}
	}

	public void cleanup() {
		if(trackMemory) {
		//	MemoryUtil.memFree(allocationInfo);
		}
	}

	void initStandardMemory() {
		try(MemoryStack stack = stackPush()) {

		}
	}


	void clearStandardMemory() {

	}

	void recreateStandardMemory() {

	}


	/**
	 *
	 * @param size the number of bytes to allocate
	 * @param usage the usage of the buffer
	 * @param memoryProperties the properties required
	 * @param returnValue long buffer --> buffer then memory
	 * @param stack memory stack to use
	 * @return the memory alignment requirements
	 */
	public long AllocateExclusive(int size,int usage,int memoryProperties,LongBuffer returnValue,MemoryStack stack) {
		VkBufferCreateInfo createInfo = VkBufferCreateInfo.mallocStack(stack);
		createInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
		createInfo.pNext(VK_NULL_HANDLE);
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
		if(vkAllocateMemory(state.renderDevice.device,allocateInfo,null,returnValue) != VK_SUCCESS) {
			vkDestroyBuffer(state.renderDevice.device,returnValue.get(oldPosition),null);
			throw new RuntimeException("Failed to create buffer backing memory");
		}
		if(vkBindBufferMemory(state.renderDevice.device,returnValue.get(oldPosition),returnValue.get(newPosition),0) != VK_SUCCESS) {
			vkDestroyBuffer(state.renderDevice.device,returnValue.get(oldPosition),null);
			vkFreeMemory(state.renderDevice.device,returnValue.get(newPosition),null);
			throw new RuntimeException("Failed to bind buffer memory");
		}
		return requirements.alignment();
	}

	public ByteBuffer mapMemory(long memoryHandle,int offset, int size,MemoryStack stack) {
		PointerBuffer pBuffer = stack.mallocPointer(0);
		if(vkMapMemory(state.renderDevice.device,memoryHandle,offset,size,0,pBuffer) != VK_SUCCESS) {
			throw new RuntimeException("Failed to map memory");
		}
		return MemoryUtil.memByteBuffer(pBuffer.get(0),size);
	}

	public void UnmapMemory(long memoryHandle) {
		vkUnmapMemory(state.renderDevice.device,memoryHandle);
	}

}
