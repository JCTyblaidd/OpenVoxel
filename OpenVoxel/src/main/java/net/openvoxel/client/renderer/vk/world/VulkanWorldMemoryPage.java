package net.openvoxel.client.renderer.vk.world;


import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.openvoxel.client.renderer.vk.core.VulkanDevice;
import net.openvoxel.client.renderer.vk.core.VulkanMemory;
import net.openvoxel.client.renderer.vk.core.VulkanUtility;
import net.openvoxel.utility.debug.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/*
 * Stores a single type of world memory
 *  of a certain memory type
 */
final class VulkanWorldMemoryPage {

	private VulkanMemory memory;
	private VulkanDevice device;
	private boolean isDeviceLocal;
	private VkBufferCreateInfo bufferCreate;

	private static final long PAGE_SIZE = VulkanMemory.MEMORY_PAGE_SIZE;
	private static final int SUB_PAGE_COUNT = 512;
	public static final int SUB_PAGE_SIZE = (int)(PAGE_SIZE / SUB_PAGE_COUNT);

	// Data:
	//  int = subPageUsage[...]
	//  short[0] = numPageAllocated, 0 if free, -1 if part of allocation
	//  short[1] = -1 if up to date, 0 if free, >0 if NUM updates till free
	// Therefore:
	//  int == 0 if Free , int > 0 if initial allocation

	private static int getAllocateData(int val) {
		return val >> 16;
	}
	private static int getUpdateData(int val) {
		return (short)(val & 0xFFFF);
	}
	private static int packData(int alloc,int update) {
		return (alloc << 16) | (update & 0xFFFF);
	}

	private TLongList allocatedPages = new TLongArrayList();
	private TLongList allocatedBuffers = new TLongArrayList();
	private TLongObjectMap<TIntList> subPageUsage = new TLongObjectHashMap<>();

	VulkanWorldMemoryPage(VulkanDevice device,VulkanMemory memory,boolean isDeviceLocal) {
		this.device = device;
		this.memory = memory;
		this.isDeviceLocal = isDeviceLocal;
		bufferCreate = VkBufferCreateInfo.malloc();
		bufferCreate.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
		bufferCreate.pNext(VK_NULL_HANDLE);
		bufferCreate.flags(0);
		bufferCreate.size(VulkanMemory.MEMORY_PAGE_SIZE);
		if(isDeviceLocal) {
			bufferCreate.usage(
					VK_BUFFER_USAGE_TRANSFER_DST_BIT |
					VK_BUFFER_USAGE_TRANSFER_SRC_BIT |
					VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
			);
		}else{
			bufferCreate.usage(
					VK_BUFFER_USAGE_TRANSFER_SRC_BIT |
					VK_BUFFER_USAGE_TRANSFER_DST_BIT
			);
		}
		//TODO: EXCLUSIVE ONLY!? (swap via pipeline barriers!)???
		//bufferCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
		//bufferCreate.pQueueFamilyIndices(null);
		if(device.familyQueue != device.familyTransfer) {
			bufferCreate.sharingMode(VK_SHARING_MODE_CONCURRENT);
			IntBuffer sharingBuffer = MemoryUtil.memAllocInt(2);
			sharingBuffer.put(0, device.familyQueue);
			sharingBuffer.put(1, device.familyTransfer);
			bufferCreate.pQueueFamilyIndices(sharingBuffer);
		}else{
			bufferCreate.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			bufferCreate.pQueueFamilyIndices(null);
		}
	}

	void close() {
		for(int i = 0; i < allocatedPages.size(); i++) {
			vkDestroyBuffer(device.logicalDevice,allocatedBuffers.get(i),null);
			memory.freeDedicatedMemory(allocatedPages.get(i));
		}
		allocatedPages.clear();
		allocatedBuffers.clear();

		if(bufferCreate.pQueueFamilyIndices() != null) {
			MemoryUtil.memFree(bufferCreate.pQueueFamilyIndices());
		}
		bufferCreate.free();
	}

	//////////////////////////
	/// Internal Functions ///
	//////////////////////////

	private long allocateNewPage() {
		long buffer;
		//int memoryType;
		//long size_required;
		try(MemoryStack stack = stackPush()) {
			LongBuffer pReturn = stack.mallocLong(1);
			int vkResult = vkCreateBuffer(device.logicalDevice,bufferCreate,null,pReturn);
			if(vkResult != VK_SUCCESS) {
				VulkanUtility.CrashOnBadResult("Failed to create world page",vkResult);
				return VK_NULL_HANDLE;
			}
			buffer = pReturn.get(0);
			/*
			VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
			vkGetBufferMemoryRequirements(device.logicalDevice,buffer,requirements);
			size_required = requirements.size();
			if(isDeviceLocal) {
				memoryType = memory.findMemoryType(
						requirements.memoryTypeBits(),
						VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
				);
			}else{
				//TODO: CHOOSE MEMORY TYPE PRIORITY!?
				memoryType = memory.findMemoryType(
						requirements.memoryTypeBits(),
						VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
	                    VK_MEMORY_PROPERTY_HOST_CACHED_BIT |
						VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
				);
				if(memoryType == -1) {
					memoryType = memory.findMemoryType(
							requirements.memoryTypeBits(),
							VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
									VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
					);
				}
			}
			*/
		}
		long page;
		//if(size_required <= VulkanMemory.MEMORY_PAGE_SIZE && false) {//TODO: DISABLE COMPLETELY!!!
			//page = memory.allocateMemoryPage(memoryType);
		//}else{
			//VulkanUtility.LogWarn("World Memory: Vulkan Page Size < Buffer Size");
			if(isDeviceLocal) {
				page = memory.allocateDedicatedBuffer(buffer,VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			}else{
				page = memory.allocateDedicatedBuffer(buffer,
						VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
								VK_MEMORY_PROPERTY_HOST_CACHED_BIT |
								VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
				);
				if(page == VK_NULL_HANDLE) {
					page = memory.allocateDedicatedBuffer(buffer,
							VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
									VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
					);
				}
			}
		//}
		if(page == VK_NULL_HANDLE) {
			throw new RuntimeException("Failed to allocate memory page");
		}
		int res = vkBindBufferMemory(device.logicalDevice,buffer,page,0);
		VulkanUtility.ValidateSuccess("Failed to bind world page memory",res);
		allocatedPages.add(page);
		allocatedBuffers.add(buffer);
		TIntList pageUsageList = new TIntArrayList();
		for(int i = 0; i < SUB_PAGE_COUNT; i++) {
			pageUsageList.add(0);
		}
		subPageUsage.put(page,pageUsageList);
		return page;
	}

	private void freePage(long page) {
		TIntList usage = subPageUsage.get(page);
		for(int i = 0; i < usage.size(); i++) {
			if(usage.get(i) != 0) {
				throw new RuntimeException("Invalid Page Free: Still in Use!");
			}
		}
		subPageUsage.remove(page);
		int offset = allocatedPages.indexOf(page);
		long buffer = allocatedBuffers.removeAt(offset);
		vkDestroyBuffer(device.logicalDevice,buffer,null);
		allocatedPages.removeAt(offset);
		memory.freeDedicatedMemory(page);
	}

	private long roundToPageCount(long size) {
		return (size + SUB_PAGE_SIZE - 1) / SUB_PAGE_SIZE;
	}

	///////////////////
	/// API Methods ///
	///////////////////

	int acquireMemoryForSize(long size) {
		int page_count = (int)roundToPageCount(size);
		if(page_count > SUB_PAGE_COUNT) {
			throw new RuntimeException("Page is too large!");
		}
		for(int alloc_idx = 0; alloc_idx < allocatedPages.size(); alloc_idx++) {
			long page_id = allocatedPages.get(alloc_idx);
			TIntList usageList = subPageUsage.get(page_id);
			int free_offset = 0;
			int free_count = 0;
			for(int sub_idx = 0; sub_idx < SUB_PAGE_COUNT; sub_idx++) {
				if(free_count == page_count) {
					break;
				}else if(usageList.get(sub_idx) == 0) {
					free_count += 1;
				}else{
					free_count = 0;
					free_offset = sub_idx + 1;
				}
			}
			if(free_count >= page_count) {
				//Found
				usageList.set(free_offset,packData(page_count,-1));
				int final_pack = packData(-1,-1);
				for(int idx = 1; idx < page_count; idx++) {
					usageList.set(free_offset + idx,final_pack);
				}
				return alloc_idx * SUB_PAGE_COUNT + free_offset;
			}
		}
		//Allocate New Page
		long new_page = allocateNewPage();
		int alloc_idx = allocatedPages.size() - 1;

		TIntList usageList = subPageUsage.get(new_page);
		usageList.set(0,packData(page_count,-1));
		int final_pack = packData(-1,-1);

		for(int idx = 1; idx < page_count; idx++) {
			usageList.set(idx,final_pack);
		}
		return alloc_idx * SUB_PAGE_COUNT;
	}

	void shrinkMemoryToSize(int page_idx, long size) {
		long page = allocatedPages.get(page_idx / SUB_PAGE_COUNT);
		int new_page_count = (int)roundToPageCount(size);
		TIntList list = subPageUsage.get(page);
		int sub_idx = page_idx % SUB_PAGE_COUNT;
		int val = list.get(sub_idx);
		int old_page_count =  getAllocateData(val);
		Validate.Condition(new_page_count <= old_page_count,"New size must be smaller than old");

		int update_data = getUpdateData(val);
		list.set(sub_idx,packData(new_page_count,update_data));
		for(int i = new_page_count; i < old_page_count; i++) {
			list.set(sub_idx + i,0);
		}
	}

	void startMemoryCountdown(int page_idx,int count_down) {
		long page = allocatedPages.get(page_idx / SUB_PAGE_COUNT);
		TIntList list = subPageUsage.get(page);
		int sub_idx = page_idx % SUB_PAGE_COUNT;
		int val = list.get(sub_idx);
		int page_count =  getAllocateData(val);
		for(int i = 0; i < page_count; i++) {
			int data = list.get(sub_idx+i);
			int _alloc = getAllocateData(data);
			int _new  = packData(_alloc,count_down);
			list.set(sub_idx+i,_new);
		}
	}

	void tickAllMemory() {
		for(int alloc_idx = 0; alloc_idx < allocatedPages.size(); alloc_idx++) {
			long page = allocatedPages.get(alloc_idx);
			TIntList list = subPageUsage.get(page);
			for(int i = 0; i < list.size(); i++) {
				int data = list.get(i);
				int _alloc = getAllocateData(data);
				int _update = getUpdateData(data);
				if(_update > 0) {
					int _new_update = _update - 1;
					if(_new_update == 0) list.set(i,0);
					else list.set(i,packData(_alloc,_new_update));
				}
			}
		}
	}

	void freeMemory(int page_idx) {
		long page = allocatedPages.get(page_idx / SUB_PAGE_COUNT);
		TIntList list = subPageUsage.get(page);
		int sub_idx = page_idx % SUB_PAGE_COUNT;
		int val = list.get(sub_idx);
		int page_count =  getAllocateData(val);
		for(int i = 0; i < page_count; i++) {
			list.set(sub_idx + i,0);
		}
	}

	///////////////////////
	// Information About //
	///////////////////////

	long getVulkanMemoryFor(int page_idx) {
		return allocatedPages.get(page_idx / SUB_PAGE_COUNT);
	}

	long getBufferFor(int page_idx) {
		return allocatedBuffers.get(page_idx / SUB_PAGE_COUNT);
	}

	long getSizeBytesFor(int page_idx) {
		long page = allocatedPages.get(page_idx / SUB_PAGE_COUNT);
		TIntList list = subPageUsage.get(page);
		int sub_idx = page_idx % SUB_PAGE_COUNT;
		int val = list.get(sub_idx);
		return getAllocateData(val) * SUB_PAGE_SIZE;
	}

	long getVulkanOffsetFor(int page_idx) {
		long sub_page = page_idx % SUB_PAGE_COUNT;
		return sub_page * SUB_PAGE_SIZE;
	}

}
