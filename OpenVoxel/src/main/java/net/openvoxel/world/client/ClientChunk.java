package net.openvoxel.world.client;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.world.chunk.Chunk;

/**
 * Created by James on 09/04/2017.
 * Client Based Chunk Implementation
 */
@SideOnly(side = Side.CLIENT)
public class ClientChunk extends Chunk {

	public ClientChunk(int x, int z) {
		super(x, z,true);
		for(int i = 0; i < 16; i++) {
			chunkSections[i] = new ClientChunkSection(this,i);
		}
	}

	public ClientChunkSection getSectionAt(int y) {
		return (ClientChunkSection)chunkSections[y];
	}

}
