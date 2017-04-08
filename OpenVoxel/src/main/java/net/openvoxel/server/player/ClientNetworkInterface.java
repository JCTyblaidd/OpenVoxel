package net.openvoxel.server.player;

import net.openvoxel.networking.ClientNetworkHandler;
import net.openvoxel.networking.protocol.AbstractPacket;

/**
 * Created by James on 08/04/2017.
 */
public class ClientNetworkInterface implements PlayerNetworkInterface {

	private ClientNetworkHandler networkHandler;
	public ClientNetworkInterface(ClientNetworkHandler handler) {
		this.networkHandler = handler;
	}

	@Override
	public void sendPacketToServer(AbstractPacket pkt) {
		networkHandler.sendPacket(pkt);
	}

	@Override
	public void sendPacketToPlayer(AbstractPacket pkt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void closeConnectionToServer() {
		networkHandler.shutdown();
	}

	@Override
	public void closeConnectionToPlayer() {
		throw new UnsupportedOperationException();
	}

}
