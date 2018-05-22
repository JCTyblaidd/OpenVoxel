package net.openvoxel.networking;

import net.openvoxel.networking.protocol.AbstractPacket;

public interface ClientNetwork {

	/**
	 * Send a packet to the server
	 *  to be processed
	 */
	void sendPacket(AbstractPacket packet);

}
