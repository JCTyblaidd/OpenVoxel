package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.renderer.vk.world.VkChunkSectionMeta;
import net.openvoxel.client.renderer.vk.world.VkWorldMemoryManager;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.AsyncBarrier;
import net.openvoxel.utility.AsyncQueue;
import net.openvoxel.utility.collection.ChunkMap;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan World Renderer
 */
public class VkWorldRenderer implements WorldRenderer {

	private VkWorldMemoryManager memoryManager;

	//Constants//
	public static final int NUM_DRAW_COMMANDS = 1;

	//Implementation Details//
	private static final int NUM_CHUNK_LOAD_SUBMITS = 2;
	private static final int NUM_CHUNK_UPDATE_SUBMITS = 2;
	private static final int NUM_CHUNK_UNLOAD_SUBMITS = 2;
	private static final int NUM_CHUNK_LOAD_OPS_PER_SUBMIT = 2;
	private static final int NUM_CHUNK_UPDATE_OPS_PER_SUBMIT = 2;
	private static final int NUM_CHUNK_UNLOAD_OPS_PER_SUBMIT = 2;

	private AsyncQueue<ClientChunkSection> chunk_load_queue = new AsyncQueue<>(1024);
	private AsyncQueue<ClientChunkSection> chunk_update_queue = new AsyncQueue<>(1024);
	private AsyncQueue<ClientChunkSection> chunk_unload_queue = new AsyncQueue<>(1024);

	private ClientWorld currentWorld;
	private EntityPlayerSP currentPlayer;

	private ReentrantReadWriteLock loaded_chunk_lock = new ReentrantReadWriteLock();
	private ChunkMap<VkChunkSectionMeta[]> loaded_chunks = new ChunkMap<>();

	VkWorldRenderer(VkDeviceState state) {
		memoryManager = new VkWorldMemoryManager(state);
	}

	void cleanup() {
		memoryManager.cleanup();
	}

	/**
	 * Must be called in vulkan exclusive context [after wait idle]
	 */
	public void shrinkMemory(int targetAmount) {
		int limit = memoryManager.reclaim_memory(targetAmount);
		//TODO: mark those that no-longer have state//
	}

	/**
	 * Must be called in vulkan exclusive context [after wait idle]
	 */
	public void growMemory() {
		memoryManager.grow_memory();
	}

	@Override
	public void onChunkLoaded(ClientChunk chunk) {
		for(int i = 0; i < 1; i++) {
			ClientChunkSection section = chunk.getSectionAt(i);
			IntBuffer copyBlock = MemoryUtil.memAllocInt(16*16*16);
			ShortBuffer copyLight = MemoryUtil.memAllocShort(16*16*16);
			MemoryUtil.memCopy(section.getBlocks(),copyBlock);
			MemoryUtil.memCopy(section.getLights(),copyLight);
			section.renderCache.set(new VkChunkSectionMeta(chunk,copyBlock,copyLight));
			chunk_load_queue.add(section);
		}
	}

	@Override
	public void onChunkDirty(ClientChunk chunk) {
		//for(int i = 0; i < 16; i++) {
		//	chunk_update_queue.add(chunk.getSectionAt(i));
		//}
	}

	@Override
	public void onChunkUnloaded(ClientChunk chunk) {
		for(int i = 0; i < 16; i++) {
			chunk_unload_queue.add(chunk.getSectionAt(i));
		}
	}

	/**
	 * Calls the Main World Rendering Code
	 */
	@Override
	public void renderWorld(EntityPlayerSP playerSP, ClientWorld worldSP) {
		currentPlayer = playerSP;
		currentWorld = worldSP;
		drawBarrier.reset(3);
		resourceBarrier.reset(NUM_CHUNK_LOAD_SUBMITS+NUM_CHUNK_UPDATE_SUBMITS+NUM_CHUNK_UNLOAD_SUBMITS);
		submissionBarrier.reset(1);
		Renderer.renderCacheManager.addWork(this::async_view);
		Renderer.renderCacheManager.addWork(this::async_shadows);
		Renderer.renderCacheManager.addWork(this::async_cube_map);
		for(int i = 0; i < NUM_CHUNK_LOAD_SUBMITS; i++) {
			Renderer.renderCacheManager.addWork(this::async_chunk_loading);
		}
		for(int i = 0; i < NUM_CHUNK_UPDATE_SUBMITS; i++) {
			Renderer.renderCacheManager.addWork(this::async_chunk_updating);
		}
		for(int i = 0; i < NUM_CHUNK_UNLOAD_SUBMITS; i++) {
			Renderer.renderCacheManager.addWork(this::async_chunk_unloading);
		}
		Renderer.renderCacheManager.addWork(this::async_transfer_generation);
		Thread.yield();
	}


	/**
	 * Run commands that need to be called from the main renderer thread
	 */
	public void prepareSubmission() {
		drawBarrier.awaitCompletion();
		//Draw commands are ready//

		submissionBarrier.awaitCompletion();
		//Resource Transfer command is ready//
	}


	/////////////////////////////////
	/// Synchronisation Variables ///
	/////////////////////////////////
	private AsyncBarrier drawBarrier = new AsyncBarrier();
	private AsyncBarrier resourceBarrier = new AsyncBarrier();
	private AsyncBarrier submissionBarrier = new AsyncBarrier();

	private void async_view() {
		//Perform Cull//

		//Perform Draw of Resources in GPU//

		//Perform Resource Acquire/Release Requests//

		//Build Command Buffer//

		drawBarrier.completeTask();
	}

	private void async_shadows() {
		//Perform Cull//

		//Perform Draw of Resources in GPU//

		//Perform Resource Acquire/Release Requests//

		//Build Command Buffer//

		drawBarrier.completeTask();
	}

	private void async_cube_map() {
		//Perform Cull//

		//Perform Draw of Resources in GPU//

		//Perform Resource Acquire/Release Requests//

		//Build Command Buffer//

		drawBarrier.completeTask();
	}

	private void async_transfer_generation() {
		resourceBarrier.awaitCompletion();
		//Sort by priority//

		//List Targets to Free//

		//List Targets to Upload//

		//Build Command Buffer//

		submissionBarrier.completeTask();
	}

	private void async_chunk_loading() {
		int to_consume = (int)chunk_load_queue.snapshotSize();
		if(to_consume > NUM_CHUNK_LOAD_OPS_PER_SUBMIT) to_consume = NUM_CHUNK_LOAD_OPS_PER_SUBMIT;
		List<ClientChunkSection> to_process = new ArrayList<>(to_consume);
		for(int i = 0; i < to_consume; i++) {
			ClientChunkSection to_load = chunk_load_queue.attemptNext();
			memoryManager.load_section_async(to_load);
			to_process.add(to_load);
		}
		resourceBarrier.completeTask();
		/*
		loaded_chunk_lock.writeLock().lock();
		for(ClientChunkSection section : to_process) {
			VkChunkSectionMeta meta = (VkChunkSectionMeta)section.renderCache.get();
			ClientChunk chunk = meta.refChunk;
			VkChunkSectionMeta[] data_array = loaded_chunks.get(chunk.chunkX,chunk.chunkZ);
			if(data_array == null) {
				data_array = new VkChunkSectionMeta[16];
				data_array[section.yIndex] = meta;
				loaded_chunks.set(chunk.chunkX,chunk.chunkZ,data_array);
			}else{
				if(data_array[section.yIndex] == null) {
					data_array[section.yIndex] = meta;
				}else{
					Logger.getLogger("Vulkan").Warning("Loaded already loaded sub-chunk");
				}
			}
		}
		loaded_chunk_lock.writeLock().unlock();
		*/
	}

	private void async_chunk_updating() {
		loaded_chunk_lock.writeLock().lock();
		int to_consume = (int)chunk_update_queue.snapshotSize();
		if(to_consume > NUM_CHUNK_UPDATE_OPS_PER_SUBMIT) to_consume = NUM_CHUNK_UPDATE_OPS_PER_SUBMIT;
		for(int i = 0; i < to_consume; i++) {
			ClientChunkSection to_update = chunk_update_queue.attemptNext();
			memoryManager.update_section_async(to_update);
		}
		resourceBarrier.completeTask();
		loaded_chunk_lock.writeLock().unlock();
	}

	private void async_chunk_unloading() {
		int to_consume = (int)chunk_unload_queue.snapshotSize();
		if(to_consume > NUM_CHUNK_UNLOAD_OPS_PER_SUBMIT) to_consume = NUM_CHUNK_UNLOAD_OPS_PER_SUBMIT;
		List<ClientChunkSection> to_process = new ArrayList<>(to_consume);
		for(int i = 0; i < to_consume; i++) {
			ClientChunkSection to_unload = chunk_unload_queue.attemptNext();
			memoryManager.unload_section_async(to_unload);
			to_process.add(to_unload);
		}
		resourceBarrier.completeTask();
		/*
		loaded_chunk_lock.writeLock().lock();
		for(ClientChunkSection section : to_process) {
			VkChunkSectionMeta meta = (VkChunkSectionMeta)section.renderCache.get();
			ClientChunk chunk = meta.refChunk;
			VkChunkSectionMeta[] data_array = loaded_chunks.get(chunk.chunkX,chunk.chunkZ);
			if(data_array != null) {
				if(data_array[section.yIndex] != null) {
					data_array[section.yIndex] = null;
					boolean is_empty = true;
					for(int i = 0; i < 16; i++) {
						if(data_array[i] != null) {
							is_empty = false;
							break;
						}
					}
					if(is_empty) {
						loaded_chunks.remove(chunk.chunkX,chunk.chunkZ);
					}
				}else{
					Logger.getLogger("Vulkan").getSubLogger("World").Warning("Unloaded sub_chunk that is not loaded");
				}
			}else{
				Logger.getLogger("Vulkan").getSubLogger("World").Warning("Unloaded sub_chunk of unloaded chunk");
			}
		}
		loaded_chunk_lock.writeLock().unlock();
		*/
	}
}
