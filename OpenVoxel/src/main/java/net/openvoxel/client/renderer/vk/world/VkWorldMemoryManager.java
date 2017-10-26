package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMappedMemoryRange;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkWorldMemoryManager {

	private VkDeviceState state;

	///DEVICE LOCAL MEMORY ALLOCATIONS//
	private static final int MEM_ALLOCATION_SIZE = 1024*1024*64; //64 Megabytes//
	private static final int MEM_CHUNK_SIZE = 1024*16;          //16 Kilobytes
	private static final int MEM_CHUNK_COUNT = MEM_ALLOCATION_SIZE / MEM_CHUNK_SIZE;
	private LongBuffer allocationList;
	private LongBuffer bufferList;
	private ByteBuffer chunkUsageInfo;

	///STAGING MEMORY ALLOCATIONS///
	private static final int MEM_STAGING_SIZE = 1024*1024*64;//64 Megabytes//
	private LongBuffer memWorldStaging;
	private ByteBuffer mappedMemory;

	public VkWorldMemoryManager(VkDeviceState state) {
		this.state = state;
		fill_chunks();
	}

	private void fill_chunks() {
		VkPhysicalDeviceMemoryProperties props = state.renderDevice.memoryProperties;
		long heap_size = 0;
		for(int i = 0; i < props.memoryHeapCount(); i++) {
			if((props.memoryHeaps(i).flags() & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
				heap_size = Math.max(heap_size,props.memoryHeaps(i).size());
			}
		}
		//TODO: improve heuristic to choose allocation count - with grow & shrink support
		long reduced_heap_size = heap_size / 16;
		int heap_allocation_count = (int)(reduced_heap_size / MEM_ALLOCATION_SIZE);
		state.vkLogger.Info("World Data Map: ",heap_allocation_count," Allocations");
		//ALLOCATE THEM BUFFERS//
		allocationList = MemoryUtil.memAllocLong(heap_allocation_count);
		bufferList = MemoryUtil.memAllocLong(heap_allocation_count);
		chunkUsageInfo = MemoryUtil.memCalloc(heap_allocation_count * MEM_CHUNK_COUNT);
		try(MemoryStack stack = stackPush()) {
			LongBuffer retValue = stack.mallocLong(2);
			for(int i = 0; i < heap_allocation_count; i++) {
				state.memoryMgr.AllocateExclusive(MEM_ALLOCATION_SIZE,
						VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
						VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, retValue, stack);

				bufferList.put(i,retValue.get(0));
				allocationList.put(i,retValue.get(1));
			}
			memWorldStaging = MemoryUtil.memAllocLong(2);
			state.memoryMgr.AllocateExclusive(MEM_STAGING_SIZE,
					VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT,
					memWorldStaging,stack);
			mappedMemory = state.memoryMgr.mapMemory(memWorldStaging.get(1),0,MEM_STAGING_SIZE,stack);
		}
	}

	/**
	 * De-allocates the latest allocation and frees the memory
	 */
	public int reclaim_memory(long targetAmount) {
		int chunks_to_remove = (int)((targetAmount + MEM_ALLOCATION_SIZE - 1) / MEM_ALLOCATION_SIZE);
		int lim = chunkUsageInfo.capacity() - MEM_CHUNK_COUNT*chunks_to_remove;
		int rem_id = bufferList.capacity()-chunks_to_remove;
		try(MemoryStack stack = stackPush()) {
			state.memoryMgr.FreeExclusive(stack.longs(bufferList.get(rem_id),allocationList.get(rem_id)));
		}
		bufferList = MemoryUtil.memRealloc(bufferList,rem_id);
		allocationList = MemoryUtil.memRealloc(allocationList,rem_id);
		return lim;
	}

	/**
	 * Allocate chunks until memory cannot grow anymore
	 */
	public void grow_memory() {
		try(MemoryStack stack = stackPush()) {
			LongBuffer retValue = stack.mallocLong(2);
			try{
				state.memoryMgr.AllocateExclusive(MEM_ALLOCATION_SIZE,
						VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
						VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, retValue, stack);
				int insert_index = bufferList.capacity();
				bufferList = MemoryUtil.memRealloc(bufferList,insert_index+1);
				allocationList = MemoryUtil.memRealloc(allocationList,insert_index+1);
				bufferList.put(insert_index,retValue.get(0));
				allocationList.put(insert_index,retValue.get(1));
			}catch(RuntimeException ignored) {}
		}
	}

	/**
	 * Free all vulkan resources on shutdown
	 */
	public void cleanup() {
		try(MemoryStack stack = stackPush()) {
			state.memoryMgr.unMapMemory(memWorldStaging.get(1));
			for (int i = 0; i < allocationList.capacity(); i++) {
				state.memoryMgr.FreeExclusive(stack.longs(bufferList.get(i),allocationList.get(i)));
			}
		}
		state.memoryMgr.FreeExclusive(memWorldStaging);
		MemoryUtil.memFree(allocationList);
		MemoryUtil.memFree(bufferList);
		MemoryUtil.memFree(chunkUsageInfo);
		MemoryUtil.memFree(memWorldStaging);
	}

	/*
	 * Synchronisation: Lazy / Sync on thread join in render thread
	 * BUT: Allocations can only happen on the same thread between syncs
	 * So no issues should occur
	 */
	public int assign_device_memory(int allocSize) {
		int chunkSize = (allocSize + MEM_CHUNK_SIZE-1)/MEM_CHUNK_SIZE;
		int ret_target = -1;
		for(int alloc = 0; alloc < allocationList.capacity(); alloc++) {
			int ok_count = 0;
			for(int chunk = 0; chunk < MEM_CHUNK_COUNT; chunk++) {
				int idx = (alloc * MEM_CHUNK_COUNT) + chunk;
				if(chunkUsageInfo.get(idx) != 0) {
					ok_count = 0;
				}else{
					ok_count++;
					if(ok_count == chunkSize) {
						ret_target = idx-chunkSize+1;
						break;
					}
				}
			}
			if(ok_count == chunkSize) {
				break;
			}
		}
		if(ret_target != -1) {
			for(int chunk = 0; chunk < chunkSize; chunk++) {
				chunkUsageInfo.put(ret_target+chunk,(byte)1);
			}
		}
		return ret_target;
	}

	/*
	 * Synchronisation: Lazy / Sync on thread join in render thread
	 */
	private void free_device_memory(int assignment,int allocSize) {
		int chunkSize = (allocSize + MEM_CHUNK_SIZE-1)/MEM_CHUNK_SIZE;
		for(int i = 0; i < chunkSize; i++) {
			chunkUsageInfo.put(assignment+i,(byte)0);
		}
	}

	public void reset_staging() {
		mappedMemory.position(0);
	}

	public int write_to_staging(ClientChunkSection section) {
		VkChunkSectionMeta meta = (VkChunkSectionMeta)section.renderCache.get();
		int pos = mappedMemory.position();
		MemoryUtil.memCopy(meta.drawDataAll,mappedMemory);
		mappedMemory.position(pos + meta.drawDataAll.capacity());
		return pos;
	}

	public void flush_staging() {
		try(MemoryStack stack = stackPush()) {
			VkMappedMemoryRange memRange = VkMappedMemoryRange.mallocStack(stack);
			memRange.sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE);
			memRange.pNext(VK_NULL_HANDLE);
			memRange.memory(memWorldStaging.get(1));
			memRange.offset(0);
			memRange.size(mappedMemory.position());
			vkFlushMappedMemoryRanges(state.renderDevice.device,memRange);
		}
	}

	public void load_section_async(ClientChunkSection to_load) {
		if(to_load == null) {
			state.vkLogger.Warning("Requested to load a null ClientChunkSection");
			return;
		}
		VkChunkSectionMeta meta = (VkChunkSectionMeta)to_load.renderCache.get();
		VkChunkRenderer chunkRenderer = new VkChunkRenderer();
		meta.drawDataAll = chunkRenderer.GenerateDrawInfo(meta.refBlocks,meta.refLighting,true);
		meta.genFromData(chunkRenderer.GenerateDrawInfo(meta.refBlocks,meta.refLighting,false));
	}

	public void unload_section_async(ClientChunkSection to_unload) {
		if(to_unload == null) {
			state.vkLogger.Warning("Requested to unload a null ClientChunkSection");
			return;
		}
		VkChunkSectionMeta meta = (VkChunkSectionMeta)to_unload.renderCache.get();
		meta.markDataAsOld();
		if(meta.isOldInMemory()) {
			free_device_memory(meta.oldDeviceLocalMemoryID,meta.oldDeviceAllocationSize);
			meta.markOldAsRemoved();
		}
	}

	public void update_section_async(ClientChunkSection to_update) {
		if(to_update == null) {
			state.vkLogger.Warning("Requested to update a null ClientChunkSection");
			return;
		}
		VkChunkSectionMeta meta = (VkChunkSectionMeta)to_update.renderCache.get();
		meta.markDataAsOld();
		VkChunkRenderer chunkRenderer = new VkChunkRenderer();
		meta.drawDataAll = chunkRenderer.GenerateDrawInfo(meta.refBlocks,meta.refLighting,true);
		meta.genFromData(chunkRenderer.GenerateDrawInfo(meta.refBlocks,meta.refLighting,false));
	}


}
