package net.openvoxel.common.event.load;

import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.common.world.World;
import net.openvoxel.common.world.chunk.Chunk;
import net.openvoxel.server.Server;

/**
 * Created by James on 08/09/2016.
 *
 * Called When A Chunk is Loaded From Disk / Created Via the WorldGen
 */
public class ChunkLoadEvent extends AbstractEvent{

	public final Server theServer;
	public final World theWorld;
	public final Chunk theChunk;
	public final boolean wasGenerated;

	public ChunkLoadEvent(Server theServer, World theWorld, Chunk theChunk,boolean wasGenerated) {
		this.theServer = theServer;
		this.theWorld = theWorld;
		this.theChunk = theChunk;
		this.wasGenerated = wasGenerated;
	}
}
