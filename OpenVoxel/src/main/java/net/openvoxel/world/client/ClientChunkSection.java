package net.openvoxel.world.client;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.utility.IRenderDataCache;
import net.openvoxel.common.block.Block;
import net.openvoxel.world.chunk.ChunkSection;

/**
 * Created by James on 09/04/2017.
 *
 * Client Side Chunk Section Implementation
 */
@SideOnly(side = Side.CLIENT)
public class ClientChunkSection extends ChunkSection {

	/**
	 * Renderer Draw Information Cache
	 */
	public IRenderDataCache renderCache = null;

	private byte metaVal;
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

	@Override
	protected void freeMemory() {
		super.freeMemory();
		if(renderCache != null) {
			renderCache.onChunkSectionFree();
		}
	}
}
