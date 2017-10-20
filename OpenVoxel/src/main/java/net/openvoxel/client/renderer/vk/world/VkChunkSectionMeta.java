package net.openvoxel.client.renderer.vk.world;

import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.world.client.ClientChunk;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class VkChunkSectionMeta implements IRenderDataCache {

	public VkChunkSectionMeta(ClientChunk chunk,IntBuffer refBlocks, ShortBuffer refLighting) {
		refChunk = chunk;
		this.refBlocks = refBlocks;
		this.refLighting = refLighting;
	}

	ClientChunk refChunk;
	IntBuffer refBlocks;
	ShortBuffer refLighting;

	ByteBuffer drawDataOpaque;
	ByteBuffer drawDataTransparent;

	int boundDeviceLocalMemoryID;
	int boundDeviceAllocationSize;
}
