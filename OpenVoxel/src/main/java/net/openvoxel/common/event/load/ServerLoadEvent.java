package net.openvoxel.common.event.load;

import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.server.Server;

/**
 * Created by James on 08/09/2016.
 *
 * Called when a new server is created or one is loaded from disk
 */
public class ServerLoadEvent extends AbstractEvent{

	public final boolean isNew;
	public final Server theServer;

	public ServerLoadEvent(boolean isNew, Server server) {
		this.isNew = isNew;
		this.theServer = server;
	}

}
