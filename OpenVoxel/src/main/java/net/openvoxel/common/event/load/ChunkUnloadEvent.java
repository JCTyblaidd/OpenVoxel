package net.openvoxel.common.event.load;

import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.World;
import net.openvoxel.server.Server;

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
