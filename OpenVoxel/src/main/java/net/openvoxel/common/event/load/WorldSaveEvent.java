package net.openvoxel.common.event.load;

import net.openvoxel.server.DedicatedServer;
import net.openvoxel.world.World;

/**
 * Created by James on 08/09/2016.
 *
 * Called before the world is saved:
 * TODO: add Save State
 */
public class WorldSaveEvent {

	public final DedicatedServer theServer;
	public final World theWorld;

	public WorldSaveEvent(DedicatedServer theServer, World theWorld) {
		this.theServer = theServer;
		this.theWorld = theWorld;
	}
}