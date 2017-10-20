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

	public ClientChunk refChunk;
	public IntBuffer refBlocks;
	public ShortBuffer refLighting;

	public ByteBuffer drawDataOpaque;
	public ByteBuffer drawDataTransparent;
}
