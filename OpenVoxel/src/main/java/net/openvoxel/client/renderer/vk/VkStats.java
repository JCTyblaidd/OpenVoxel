package net.openvoxel.client.renderer.vk;

import gnu.trove.map.TLongByteMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongByteHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import net.openvoxel.statistics.SystemStatistics;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.vulkan.VK10.*;

public class VkStats {

	//Enabled Flag//
	private static final boolean VULKAN_TRACK_MEMORY = true;

	//References//
	private static AtomicLong vulkanDeviceMemory = SystemStatistics.updateGraphicsGPUMemUsage;
	private static AtomicLong vulkanHostMemory = SystemStatistics.updateGraphicsLocalMemUsage;

	//End Times//
	private static long last_terminate_timestamp = 0;
	private static TLongLongMap memSizeMap = new TLongLongHashMap();
	private static TLongByteMap memDeviceMap = new TLongByteHashMap();

	private static boolean isDeviceType(int memIndex) {
		VkPhysicalDeviceMemoryProperties props = VkRenderer.Vkrenderer.getDeviceState().renderDevice.memoryProperties;
		return (props.memoryTypes(memIndex).propertyFlags() & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0;
	}

	private static void addMemory(VkMemoryAllocateInfo pAllocateInfo,long memory) {
		boolean isDevice = isDeviceType(pAllocateInfo.memoryTypeIndex());
		final long size = pAllocateInfo.allocationSize();
		memSizeMap.put(memory,size);
		memDeviceMap.put(memory,(byte)(isDevice ? 1 : 0));
		if(isDevice) {
			vulkanDeviceMemory.getAndAdd(size);
		}else{
			vulkanHostMemory.getAndAdd(size);
		}
	}

	private static void freeMemory(long memory) {
		boolean isDevice = memDeviceMap.remove(memory) != 0;
		long size = memSizeMap.remove(memory);
		if(isDevice) {
			vulkanDeviceMemory.getAndAdd(-size);
		}else{
			vulkanHostMemory.getAndAdd(-size);
		}
	}

	public static int AllocMemory(VkDevice device, VkMemoryAllocateInfo pAllocateInfo, VkAllocationCallbacks pAllocator,LongBuffer pMemory) {
		int result = vkAllocateMemory(device,pAllocateInfo,pAllocator,pMemory);
		if(VULKAN_TRACK_MEMORY && result == VK_SUCCESS) {
			addMemory(pAllocateInfo, pMemory.get(pMemory.position()));
		}
		return result;
	}

	public static void FreeMemory(VkDevice device, long memory,VkAllocationCallbacks pAllocator) {
		if(VULKAN_TRACK_MEMORY) {
			freeMemory(memory);
		}
		vkFreeMemory(device,memory,pAllocator);
	}

	public static void PushGraphicsTimestamp(long start, long end) {
		long time_usage = end - start;
		long time_total = end - last_terminate_timestamp;
		last_terminate_timestamp = end;
		double perc_usage = (double)time_usage / time_total;
		double last_graphics = SystemStatistics.graphics_history[SystemStatistics.write_index];
		SystemStatistics.graphics_history[SystemStatistics.write_index] = (perc_usage+last_graphics)/2;
	}

}
