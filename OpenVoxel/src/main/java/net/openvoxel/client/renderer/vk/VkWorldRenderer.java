package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.renderer.vk.world.VkChunkSectionMeta;
import net.openvoxel.client.renderer.vk.world.VkWorldMemoryManager;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.AsyncQueue;
import net.openvoxel.utility.AsyncTriQueue;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Semaphore;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan World Renderer
 */
public class VkWorldRenderer implements WorldRenderer {

	public VkWorldMemoryManager memoryManager;

	private AsyncQueue<ClientChunkSection> chunk_load_queue = new AsyncQueue<>(1024);
	private AsyncQueue<ClientChunkSection> chunk_unload_queue = new AsyncQueue<>(1024);
	private AsyncQueue<ClientChunkSection> chunk_update_queue = new AsyncQueue<>(1024);


	VkWorldRenderer(VkDeviceState state) {
		memoryManager = new VkWorldMemoryManager(state);
	}

	void cleanup() {
		//memoryManager.cleanup();
	}

	@Override
	public void renderWorld(EntityPlayerSP playerSP, ClientWorld worldSP) {
		//TODO: add condition variables//
		Renderer.renderCacheManager.addWork(() -> {
			//Perform view culling//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform shadow map culling//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform CubMap culling//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Chunk Section Loading//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Chunk Section Unloading//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Chunk Section Updating//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Command Buffer Generation - View//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Command Buffer Generation - Shadow//
		});
		Renderer.renderCacheManager.addWork(() -> {
			//Perform Command Buffer Generation - CubeMap//
		});
	}


	/**
	 * Run commands that need to be called from the main renderer thread
	 */
	public void prepareSubmission() {

	}

	@Override
	public void onChunkLoaded(ClientChunk chunk) {
		/*
		for(int i = 0; i < 16; i++) {
			ClientChunkSection section = chunk.getSectionAt(i);
			IntBuffer copyBlock = MemoryUtil.memAllocInt(16*16*16);
			ShortBuffer copyLight = MemoryUtil.memAllocShort(16*16*16);
			MemoryUtil.memCopy(section.getBlocks(),copyBlock);
			MemoryUtil.memCopy(section.getLights(),copyLight);
			section.renderCache.set(new VkChunkSectionMeta(chunk,copyBlock,copyLight));
			chunk_load_queue.add(section);
		}
		*/
	}

	@Override
	public void onChunkDirty(ClientChunk chunk) {
		/*
		for(int i = 0; i < 16; i++) {
			chunk_update_queue.add(chunk.getSectionAt(i));
		}
		*/
	}

	@Override
	public void onChunkUnloaded(ClientChunk chunk) {
		/*
		for(int i = 0; i < 16; i++) {
			chunk_unload_queue.add(chunk.getSectionAt(i));
		}
		*/
	}
}
