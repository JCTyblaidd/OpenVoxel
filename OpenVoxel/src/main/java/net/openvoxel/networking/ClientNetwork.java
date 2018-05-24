package net.openvoxel.networking;

import net.openvoxel.networking.protocol.AbstractPacket;

import java.io.Closeable;
import java.util.Iterator;

public interface ClientNetwork extends Closeable {

	@Override
	void close();

	/**
	 * Send a packet to the server
	 *  to be processed
	 */
	void sendPacketToServer(AbstractPacket packet);

	Iterator<AbstractPacket> getPacketsFromServer();
}
