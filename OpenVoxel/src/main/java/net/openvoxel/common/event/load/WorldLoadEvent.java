package net.openvoxel.common.event.load;

import net.openvoxel.server.DedicatedServer;
import net.openvoxel.world.World;

/**
 * Created by James on 08/09/2016.
 *
 * Called When a World is Loaded From Disk
 * TODO: add Load State
 */
public class WorldLoadEvent {

	public final DedicatedServer theServer;
	public final World theWorld;

	public WorldLoadEvent(DedicatedServer theServer, World theWorld) {
		this.theServer = theServer;
		this.theWorld = theWorld;
	}
}
