package net.openvoxel.server.player;

import net.openvoxel.networking.protocol.AbstractPacket;

/**
 * Interface Referenced by Entity Players
 */
public interface PlayerNetworkInterface {

	void sendPacketToServer(AbstractPacket pkt);

	void sendPacketToPlayer(AbstractPacket pkt);

	void closeConnectionToServer();

	void closeConnectionToPlayer();
}
