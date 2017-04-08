package net.openvoxel.server.player;

import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.server.ServerPlayerConnection;

/**
 * Created by James on 08/04/2017.
 *
 */
public class RemoteNetworkInterface implements PlayerNetworkInterface {

	private ServerPlayerConnection playerConnection;

	public RemoteNetworkInterface(ServerPlayerConnection connection) {
		this.playerConnection = connection;
	}

	@Override
	public void sendPacketToServer(AbstractPacket pkt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendPacketToPlayer(AbstractPacket pkt) {
		playerConnection.sendPacket(pkt);
	}

	@Override
	public void closeConnectionToServer() {

	}

	@Override
	public void closeConnectionToPlayer() {

	}
}
