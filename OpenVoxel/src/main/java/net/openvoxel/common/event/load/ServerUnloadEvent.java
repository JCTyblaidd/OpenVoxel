package net.openvoxel.common.event.load;

import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.server.Server;

/**
 * Created by James on 08/09/2016.
 */
public class ServerUnloadEvent extends AbstractEvent{

	public final Server theServer;

	public ServerUnloadEvent(Server theServer) {
		this.theServer = theServer;
	}
}
