package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.world.client.ClientChunkSection;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkWorldMemoryManager {

	private VkDeviceState state;

	///DEVICE LOCAL MEMORY ALLOCATIONS//
	private static final int MEM_ALLOCATION_SIZE = 1024*1024*64; //64 Megabytes//
	private static final int MEM_CHUNK_SIZE = 1024*512;          //512 Kilobytes
	private static final int MEM_CHUNK_COUNT = MEM_ALLOCATION_SIZE / MEM_CHUNK_SIZE;
	private LongBuffer allocationList;
	private LongBuffer bufferList;
	private ByteBuffer chunkUsageInfo;

	///STAGING MEMORY ALLOCATIONS///
	private static final int MEM_STAGING_SIZE = 1024*1024*64;//64 Megabytes//


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
		//TODO: improve heuristic to choose allocation count
		long reduced_heap_size = (heap_size * 48) / 64;
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
		}
	}

	public void cleanup() {
		try(MemoryStack stack = stackPush()) {
			for (int i = 0; i < allocationList.capacity(); i++) {
				state.memoryMgr.FreeExclusive(stack.longs(bufferList.get(i),allocationList.get(i)));
			}
		}
		MemoryUtil.memFree(allocationList);
		MemoryUtil.memFree(bufferList);
		MemoryUtil.memFree(chunkUsageInfo);
	}

	private int assign_device_memory(int allocSize) {
		return 0;
	}

	private void free_device_memory(int assignment,int allocSize) {

	}

	public void load_section_async(ClientChunkSection to_load) {
		if(to_load == null) {
			System.out.println("ERROR: NULL SECTION LOAD!!!");
			return;
		}
		VkChunkSectionMeta meta = (VkChunkSectionMeta)to_load.renderCache.get();
		VkChunkRenderer chunkRenderer = new VkChunkRenderer();
		meta.drawDataOpaque = chunkRenderer.GenerateDrawInfo(meta.refBlocks,meta.refLighting,true);
		meta.drawDataTransparent = chunkRenderer.GenerateDrawInfo(meta.refBlocks,meta.refLighting,false);
		MemoryUtil.memFree(meta.refBlocks);
		MemoryUtil.memFree(meta.refLighting);
		//Load//

		//DEBUG : CLEANUP//
		if(meta.drawDataOpaque != null) {
			MemoryUtil.memFree(meta.drawDataOpaque);
		}
		if(meta.drawDataTransparent != null){
			MemoryUtil.memFree(meta.drawDataTransparent);
		}
	}

	public void unload_section_async(ClientChunkSection to_unload) {
		VkChunkSectionMeta meta = (VkChunkSectionMeta)to_unload.renderCache.get();
		//Unload//

	}

	public void update_section_async(ClientChunkSection to_update) {
		VkChunkSectionMeta meta = (VkChunkSectionMeta)to_update.renderCache.get();
		System.out.println("Section Update Not Implemented Yet");
		MemoryUtil.memFree(meta.refBlocks);
		MemoryUtil.memFree(meta.refLighting);
		//UPDATE - TODO: AFTER IMPLEMENTING//
	}


}
