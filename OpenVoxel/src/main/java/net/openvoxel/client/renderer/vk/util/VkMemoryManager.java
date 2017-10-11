package net.openvoxel.client.renderer.vk.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.renderer.vk.VkGUIRenderer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

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

	public LongBuffer memImageStaging;

	//Used by world renderer//
	public LongBuffer memChunks;


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
			AllocateExclusive(VkGUIRenderer.GUI_BUFFER_SIZE, VK_BUFFER_USAGE_TRANSFER_SRC_BIT
					,VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
					,memGuiStaging,stack);
			AllocateExclusive(VkGUIRenderer.GUI_BUFFER_SIZE, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
					,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, memGuiDrawing,stack);
		}
	}


	void clearStandardMemory() {
		FreeExclusive(memGuiStaging);
		FreeExclusive(memGuiDrawing);
		MemoryUtil.memFree(memGuiStaging);
		MemoryUtil.memFree(memGuiDrawing);
	}

	void recreateStandardMemory() {
		//NO OP//
	}

	/////////////////////////
	/// Utility Functions ///
	/////////////////////////

	public void AllocateImage(int usage,int memoryProperties,LongBuffer returnValue,MemoryStack stack) {
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
		returnValue.position(oldPosition);
		return requirements.alignment();
	}

	public void FreeExclusive(LongBuffer value) {
		vkDestroyBuffer(state.renderDevice.device,value.get(0),null);
		vkFreeMemory(state.renderDevice.device,value.get(1),null);
	}

	public ByteBuffer mapMemory(long memoryHandle,int offset, int size,MemoryStack stack) {
		PointerBuffer pBuffer = stack.mallocPointer(1);
		if(vkMapMemory(state.renderDevice.device,memoryHandle,offset,size,0,pBuffer) != VK_SUCCESS) {
			throw new RuntimeException("Failed to map memory");
		}
		return MemoryUtil.memByteBuffer(pBuffer.get(0),size);
	}

	public void unMapMemory(long memoryHandle) {
		vkUnmapMemory(state.renderDevice.device,memoryHandle);
	}

}
