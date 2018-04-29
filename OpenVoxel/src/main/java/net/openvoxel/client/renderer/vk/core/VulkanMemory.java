package net.openvoxel.client.renderer.vk.core;

import gnu.trove.map.TLongByteMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongByteHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryHeap;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanMemory {

	//Constant State
	private final VulkanDevice device;
	private final long totalHostMemory;
	private final long totalDeviceMemory;

	//Dynamic State
	private long usedHost;
	private long usedDevice;

	//Memory Metadata
	private TLongLongMap memorySizeMap = new TLongLongHashMap();
	private TLongByteMap memoryHostMap = new TLongByteHashMap();


	// 16MB = size of 1 memory page
	public static final long MEMORY_PAGE_SIZE = 16 * 1024 * 1024;

	//////////////////////
	/// Initialization ///
	//////////////////////

	VulkanMemory(VulkanDevice device) {
		this.device = device;
		long totalHost = 0L;
		long totalDevice = 0L;
		for(int i = 0; i < device.memory.memoryHeapCount(); i++) {
			VkMemoryHeap heap = device.memory.memoryHeaps(i);
			if((heap.flags() & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
				totalDevice += heap.size();
			}else{
				totalHost += heap.size();
			}
		}
		totalHostMemory = totalHost;
		totalDeviceMemory = totalDevice;
		usedHost = 0L;
		usedDevice = 0L;
	}

	public void close() {
		//Cleanup//
		if(usedHost != 0L) {
			VulkanUtility.LogSevere("Cleaning vulkan memory w/ usedHost != 0");
		}
		if(usedDevice != 0L) {
			VulkanUtility.LogSevere("Cleaning vulkan memory w/ usedDevice != 0");
		}
		VulkanUtility.LogSevere("Listing Bad Memory Pages");
		for(long allocation : memoryHostMap.keys()) {
			boolean isDeviceLocal = memoryHostMap.get(allocation) != 0;
			long allocationSize = memorySizeMap.get(allocation);
			if(allocationSize == memorySizeMap.getNoEntryValue()) {
				VulkanUtility.LogSevere(" - #" + Long.toHexString(allocation) +
						                        ": MEMORY-PAGE @ " +
						                        (isDeviceLocal ? "Device" : "Host"));
			}else{
				VulkanUtility.LogSevere(" - #" + Long.toHexString(allocation) +
												": Size = " + Long.toString(allocationSize) +
						                        " @ " + (isDeviceLocal ? "Device" : "Host"));
			}
		}
	}

	/////////////////////////
	/// Memory Management ///
	/////////////////////////


	private long allocateMemory(long size,int typeIndex,boolean track) {
		try(MemoryStack stack = stackPush()) {
			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.mallocStack(stack);
			allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			allocateInfo.pNext(VK_NULL_HANDLE);
			allocateInfo.allocationSize(size);
			allocateInfo.memoryTypeIndex(typeIndex);
			LongBuffer retVal = stack.mallocLong(1);
			int result = vkAllocateMemory(device.logicalDevice,allocateInfo,null,retVal);
			if(result == VK_SUCCESS) {
				boolean isDeviceLocal = (device.memory.memoryTypes(typeIndex).propertyFlags()
						                         & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0;
				if(isDeviceLocal) {
					usedDevice += size;
				}else{
					usedHost += size;
				}
				long allocation = retVal.get(0);
				if(track) {
					memorySizeMap.put(allocation,size);
					memoryHostMap.put(allocation,(byte)(isDeviceLocal ? 1 : 0));
				}
				return allocation;
			}else if(result == VK_ERROR_TOO_MANY_OBJECTS) {
				VulkanUtility.LogInfo("Failed to Allocate Memory: Too Many Objects");
			}else if(result == VK_ERROR_OUT_OF_DEVICE_MEMORY) {
				VulkanUtility.LogInfo("Failed to Allocate Memory: Out of Device Memory");
			}else if(result == VK_ERROR_OUT_OF_HOST_MEMORY) {
				VulkanUtility.LogInfo("Failed to Allocate Memory: Out of Host Memory");
			}else{
				VulkanUtility.CrashOnBadResult("Unexpected Value from vkAllocateMemory",result);
			}
			return VK_NULL_HANDLE;
		}
	}

	///////////////////////////
	/// Interface Functions ///
	///////////////////////////

	public int findMemoryType(int typeFilter, int requiredProperties) {
		for(int i = 0; i < device.memory.memoryTypeCount(); i++) {
			int propertyFlags = device.memory.memoryTypes(i).propertyFlags();
			boolean allowed_type = (typeFilter & (1 << i)) != 0;
			boolean contains_props = (propertyFlags & requiredProperties) == requiredProperties;
			if(allowed_type && contains_props) return i;
		}
		return -1;
	}

	public long allocateMemoryPage(int typeIndex) {
		long allocation =  allocateMemory(MEMORY_PAGE_SIZE,typeIndex,false);
		boolean isDeviceLocal = (device.memory.memoryTypes(typeIndex).propertyFlags() & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0;
		memoryHostMap.put(allocation,(byte)(isDeviceLocal ? 1 : 0));
		return allocation;
	}

	public void freeMemoryPage(long memoryPage) {
		vkFreeMemory(device.logicalDevice,memoryPage,null);
		boolean isDeviceLocal = memoryHostMap.remove(memoryPage) != 0;
		if(isDeviceLocal) {
			usedDevice -= MEMORY_PAGE_SIZE;
		}else{
			usedHost -= MEMORY_PAGE_SIZE;
		}
	}

	public void freeDedicatedMemory(long allocation) {
		vkFreeMemory(device.logicalDevice,allocation,null);
		long size = memorySizeMap.remove(allocation);
		boolean isDeviceLocal = memoryHostMap.remove(allocation) != 0;
		if(isDeviceLocal) {
			usedDevice -= size;
		}else{
			usedHost -= size;
		}
	}

	public long allocateDedicatedImage(long vulkanImage, int requiredProperties) {
		try(MemoryStack stack = stackPush()) {
			VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
			vkGetImageMemoryRequirements(device.logicalDevice,vulkanImage,requirements);
			int memoryTypeIdx = findMemoryType(requirements.memoryTypeBits(),requiredProperties);
			if(memoryTypeIdx == -1) return VK_NULL_HANDLE;
			return allocateMemory(requirements.size(),memoryTypeIdx,true);
		}
	}

	public long allocateDedicatedBuffer(long vulkanBuffer, int requiredProperties) {
		try(MemoryStack stack = stackPush()) {
			VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
			vkGetBufferMemoryRequirements(device.logicalDevice,vulkanBuffer,requirements);
			int memoryTypeIdx = findMemoryType(requirements.memoryTypeBits(),requiredProperties);
			if(memoryTypeIdx == -1) return VK_NULL_HANDLE;
			return allocateMemory(requirements.size(),memoryTypeIdx,true);
		}
	}
}
