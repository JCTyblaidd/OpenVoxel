package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.world.client.ClientChunk;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;

public class VkChunkSectionMeta implements IRenderDataCache {

	public VkChunkSectionMeta(ClientChunk chunk,IntBuffer refBlocks, ShortBuffer refLighting) {
		refChunk = chunk;
		this.refBlocks = refBlocks;
		this.refLighting = refLighting;
	}

	/*
	 * Pointer to relevant chunk
	 */
	public ClientChunk refChunk;

	/*
	 * Cache of the latest block information [cleaned on draw]
	 */
	IntBuffer refBlocks;

	/*
	 * Cache of the latest lighting information [cleaned on draw]
	 */
	ShortBuffer refLighting;

	/*
	 * Cache of the latest draw information
	 */
	ByteBuffer drawDataAll;

	/*
	 * The number of triangles in an opaque draw operation
	 */
	int drawCountOpaque;

	/*
	 * The number of triangles in a transparent draw operation
	 */
	int drawCountTransparent;

	/*
	 * Old Device-local storage index (-1 if not stored)
	 */
	int oldDeviceLocalMemoryID = -1;

	/*
	 * Old device-local storage
	 */
	int oldDeviceAllocationSize = 0;

	/*
	 * Device-local storage index (-1 if not stored)
	 */
	int boundDeviceLocalMemoryID;

	/*
	 * Device-local current allocation size (meaningless if boundDeviceLocalMem
	 */
	int boundDeviceAllocationSize;

	/*
	 * Is the latest version stored in device-local memory
	 */
	boolean latestInMemory;

	boolean isOldInMemory() {
		return oldDeviceLocalMemoryID != -1;
	}

	void markOldAsRemoved() {
		oldDeviceLocalMemoryID = -1;
		oldDeviceAllocationSize = 0;
	}

	void markDataAsOld() {
		MemoryUtil.memFree(drawDataAll);
		drawDataAll = null;
		oldDeviceAllocationSize = boundDeviceAllocationSize;
		oldDeviceLocalMemoryID = boundDeviceLocalMemoryID;
		boundDeviceLocalMemoryID = -1;
		boundDeviceAllocationSize = 0;
	}

	void genFromData(ByteBuffer drawDataTransparent) {
		MemoryUtil.memFree(refBlocks);
		MemoryUtil.memFree(refLighting);
		if(drawDataAll != null) {
			drawCountOpaque = drawDataAll.position();
			drawDataAll.position(0);
			if(drawDataTransparent != null) {
				drawCountTransparent = drawDataTransparent.position();
				drawDataTransparent.position(0);
				drawDataAll = MemoryUtil.memRealloc(drawDataAll,drawDataAll.capacity()+drawDataTransparent.capacity());
				drawDataAll.position(drawCountOpaque);
				drawDataAll.put(drawDataTransparent);
				drawDataAll.position(0);
				MemoryUtil.memFree(drawDataTransparent);
			}else{
				drawCountTransparent = 0;
			}
		}else{
			drawCountOpaque = 0;
			drawDataAll = drawDataTransparent;
			if(drawDataAll != null) {
				drawCountTransparent = drawDataAll.position();
				drawDataAll.position(0);
			}else{
				drawCountTransparent = 0;
			}
		}
		//Update Flag - if empty --> latest is in memory//
		latestInMemory = drawDataAll == null;
		boundDeviceLocalMemoryID = -1;
		boundDeviceAllocationSize = 0;
	}


	void cmdBindVertexBuffersOpaque(VkCommandBuffer cmd, long buffer, int off,MemoryStack stack) {
		final int num = drawCountOpaque;
		vkCmdBindVertexBuffers(cmd,0,
				stack.longs(buffer,buffer,buffer,buffer,buffer,buffer,buffer),
				stack.longs(off,off+num*12,off+num*16,off+num*19,off+num*22,off+num*26,off+num*30));
	}

	void cmdBindVertexBuffersTransparent(VkCommandBuffer cmd, long buffer, int offset, MemoryStack stack) {
		final int num = drawCountTransparent;
		final int off = drawCountOpaque + offset;
		vkCmdBindVertexBuffers(cmd,0,
				stack.longs(buffer,buffer,buffer,buffer,buffer,buffer,buffer),
				stack.longs(off,off+num*12,off+num*16,off+num*19,off+num*22,off+num*26,off+num*30));
	}

	void cmdDrawOpaque(VkCommandBuffer cmd) {
		vkCmdDraw(cmd,drawCountOpaque,1,0,0);
	}

	void cmdDrawTransparent(VkCommandBuffer cmd) {
		vkCmdDraw(cmd,drawCountTransparent,1,0,0);
	}
}
