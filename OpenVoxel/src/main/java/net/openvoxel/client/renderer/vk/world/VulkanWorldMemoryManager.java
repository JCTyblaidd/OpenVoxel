package net.openvoxel.client.renderer.vk.world;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

/**
 * Manages Memory for World Chunks
 *  With Each stored block having a state:
 *   UP_TO_DATE
 *   OUT_OF_DATE [int countdown]
 *   FREE
 *
 *  Page Types [HOST,DEVICE]
 *  Transfer Between (Bidirectional...)
 */
public class VulkanWorldMemoryManager {

	private VulkanWorldMemoryPage pageDeviceLocal;
	private VulkanWorldMemoryPage pageHostVisible;
	private TLongObjectMap<ByteBuffer> mappedMemory;
	private TLongIntMap memoryUsageMap;
	private VkDevice device;

	VulkanWorldMemoryManager(VulkanDevice device, VulkanMemory memory) {
		this.device = device.logicalDevice;
		pageDeviceLocal = new VulkanWorldMemoryPage(device,memory,true);
		pageHostVisible = new VulkanWorldMemoryPage(device,memory,false);
		mappedMemory = new TLongObjectHashMap<>();
		memoryUsageMap = new TLongIntHashMap();
	}
	
	public void close() {
		pageHostVisible.close();
		pageDeviceLocal.close();
	}

	public void tick() {//TODO: CALL THE TICK METHOD
		pageHostVisible.tickAllMemory();
		pageDeviceLocal.tickAllMemory();
	}

	///
	/// Acquire & update new chunk data
	///

	public synchronized int allocHostMemory(long size) {
		return pageHostVisible.acquireMemoryForSize(size);
	}

	public synchronized void shrinkHostMemory(int memory,long new_size) {
		pageHostVisible.shrinkMemoryToSize(memory,new_size);
	}

	public synchronized long getOffsetForHost(int memory) {
		return pageHostVisible.getVulkanOffsetFor(memory);
	}

	///
	/// Memory Map
	///

	public synchronized ByteBuffer mapHostMemory(int memory) {
		try(MemoryStack stack = stackPush()) {
			long vkMem = pageHostVisible.getVulkanMemoryFor(memory);
			ByteBuffer buf = mappedMemory.get(vkMem);
			if(buf != null) {
				memoryUsageMap.increment(vkMem);
				return buf;
			}else{
				PointerBuffer pData = stack.mallocPointer(1);
				int vkResult = vkMapMemory(device,vkMem,0,VulkanMemory.MEMORY_PAGE_SIZE,0,pData);
				VulkanUtility.ValidateSuccess("Failed to map: World Memory",vkResult);
				buf = pData.getByteBuffer((int)VulkanMemory.MEMORY_PAGE_SIZE);
				mappedMemory.put(vkMem,buf);
				memoryUsageMap.put(vkMem,1);
				return buf;
			}
		}
	}

	public synchronized void unMapHostMemory(int memory) {
		try(MemoryStack stack = stackPush()) {
			long vkMem = pageHostVisible.getVulkanMemoryFor(memory);
			int new_usage = memoryUsageMap.get(vkMem) - 1;
			if(new_usage == 0) {
				memoryUsageMap.remove(vkMem);
				mappedMemory.remove(vkMem);
				vkUnmapMemory(device,vkMem);
			}else{
				memoryUsageMap.put(vkMem,new_usage);
			}
		}
	}

	public synchronized long GetHostBuffer(int memory) {
		return pageHostVisible.getBufferFor(memory);
	}

	public synchronized void FreeHostMemory(int memory,int swap_size) {
		pageHostVisible.startMemoryCountdown(memory,swap_size);
	}

	public synchronized void InvalidateHostMemory(int memory) {
		pageHostVisible.freeMemory(memory);
	}

	///
	/// Device Memory Management
	///

	public synchronized int GetDeviceMemory(int host_memory) {
		long size = pageHostVisible.getSizeBytesFor(host_memory);
		return pageDeviceLocal.acquireMemoryForSize(size);
	}

	public synchronized long GetDeviceBuffer(int device_memory) {
		return pageDeviceLocal.getBufferFor(device_memory);
	}

	public synchronized long GetDeviceOffset(int device_memory) {
		return pageDeviceLocal.getVulkanOffsetFor(device_memory);
	}

	public synchronized long GetDeviceSize(int device_memory) {
		return pageDeviceLocal.getSizeBytesFor(device_memory);
	}

	public synchronized void FreeMemoryFromDevice(int device_memory,int swap_size) {
		pageDeviceLocal.startMemoryCountdown(device_memory,swap_size);
	}

}
