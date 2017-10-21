package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.world.client.ClientChunk;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;

public class VkChunkSectionMeta implements IRenderDataCache {

	public VkChunkSectionMeta(ClientChunk chunk,IntBuffer refBlocks, ShortBuffer refLighting) {
		refChunk = chunk;
		this.refBlocks = refBlocks;
		this.refLighting = refLighting;
	}

	ClientChunk refChunk;
	IntBuffer refBlocks;
	ShortBuffer refLighting;

	ByteBuffer drawDataAll;

	int drawCountOpaque;
	int drawCountTransparent;

	int boundDeviceLocalMemoryID;
	int boundDeviceAllocationSize;
	boolean latestInMemory;

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
		if(drawDataAll != null) {
			boundDeviceAllocationSize = drawDataAll.capacity();
		}else{
			boundDeviceAllocationSize = 0;
		}
		//Update Flag//
		latestInMemory = drawDataAll != null;
	}

	void cmdBindVertexBuffersOpaque(VkCommandBuffer cmd, long buffer, MemoryStack stack) {
		final int num = drawCountOpaque;
		vkCmdBindVertexBuffers(cmd,0,
				stack.longs(buffer,buffer,buffer,buffer,buffer,buffer,buffer),
				stack.longs(0,num*12,num*16,num*19,num*22,num*26,num*30));
	}

	void cmdBindVertexBuffersTransparent(VkCommandBuffer cmd, long buffer, MemoryStack stack) {
		final int num = drawCountTransparent;
		final int off = drawCountOpaque;
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
