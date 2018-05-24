package net.openvoxel.networking;

import net.openvoxel.networking.protocol.AbstractPacket;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Represents the server connection manager
 */
public interface ServerNetwork extends Closeable {

	@Override
	void close();

	/**
	 * Represents a connected to a client from the server
	 */
	interface ConnectedClient {

		boolean isLocalPlayer();

		void sendPacketToClient(AbstractPacket packet);

		Iterator<AbstractPacket> getPacketsFromClient();

		void forceDisconnect();
	}
}
