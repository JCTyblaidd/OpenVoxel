package net.openvoxel.world.client;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.common.block.Block;
import net.openvoxel.world.chunk.Chunk;
import net.openvoxel.world.chunk.ChunkSection;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by James on 09/04/2017.
 *
 * Client Side Chunk Section Implementation
 */
@SideOnly(side = Side.CLIENT)
public class ClientChunkSection extends ChunkSection {

	///
	/// Renderer Information
	///
	private boolean isDrawDirty = true;
	public int Renderer_Info_Opaque = 0;
	public int Renderer_Size_Opaque = -1;
	public int Renderer_Info_Transparent = 0;
	public int Renderer_Size_Transparent = -1;

	private byte metaVal;

	public ClientChunkSection(ClientChunk refChunk,int idx) {
		super(refChunk,idx);
	}

	public ClientChunk getChunk() {
		return (ClientChunk)refChunk;
	}

	public int RawDataAt(int x, int y, int z) {
		int reqIndex = x * 256 + y * 16 + z;
		return blockInformation.get(reqIndex);
	}

	public Block blockAt(int x, int y, int z) {
		int reqIndex = x * 256 + y * 16 + z;
		int val = blockInformation.get(reqIndex);
		Block oldBlock = OpenVoxel.getInstance().blockRegistry.getBlockFromID(val >> 8);
		metaVal = (byte)(val & 0xFF);
		return oldBlock;
	}
	public byte getPrevMeta() {
		return metaVal;
	}

	public final IntBuffer getBlocks() {
		return blockInformation;
	}

	public final ShortBuffer getLights() {
		return this.blockLightInfo;
	}

	/*
	@Override
	protected void freeMemory() {
		super.freeMemory();
		if(renderCache.get() != null) {
			renderCache.get().onChunkSectionFree();
		}
	}
	*/

	public boolean isDirty() {
		return isDrawDirty;
	}

	public void markClean() {
		isDrawDirty = false;
	}

	public boolean isEmpty() {
		return false;
	}
}
