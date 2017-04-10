package net.openvoxel.common.event.load;

import net.openvoxel.world.World;
import net.openvoxel.server.Server;

/**
 * Created by James on 08/09/2016.
 *
 * Called before the world is saved:
 * TODO: add Save State
 */
public class WorldSaveEvent {

	public final Server theServer;
	public final World theWorld;

	public WorldSaveEvent(Server theServer, World theWorld) {
		this.theServer = theServer;
		this.theWorld = theWorld;
	}
}