package net.openvoxel.common.event.load;

import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.server.DedicatedServer;

/**
 * Created by James on 08/09/2016.
 */
public class ServerUnloadEvent extends AbstractEvent{

	public final DedicatedServer theServer;

	public ServerUnloadEvent(DedicatedServer theServer) {
		this.theServer = theServer;
	}
}
