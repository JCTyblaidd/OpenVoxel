package net.openvoxel.common.event.load;

import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.server.Server;
import net.openvoxel.world.World;
import net.openvoxel.world.chunk.Chunk;

/**
 * Created by James on 08/09/2016.
 *
 * Called when a chunk is unloaded from memory
 */
public class ChunkUnloadEvent extends AbstractEvent {

	public final Server theServer;
	public final World theWorld;
	public final Chunk theChunk;

	public ChunkUnloadEvent(Server theServer, World theWorld, Chunk theChunk) {
		this.theServer = theServer;
		this.theWorld = theWorld;
		this.theChunk = theChunk;
	}

}
