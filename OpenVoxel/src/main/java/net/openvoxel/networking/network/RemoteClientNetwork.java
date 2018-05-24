package net.openvoxel.networking.network;

import net.openvoxel.networking.ClientNetwork;
import net.openvoxel.networking.protocol.AbstractPacket;

import java.util.Iterator;

public class RemoteClientNetwork implements ClientNetwork {
	@Override
	public void close() {

	}

	@Override
	public void sendPacketToServer(AbstractPacket packet) {

	}

	@Override
	public Iterator<AbstractPacket> getPacketsFromServer() {
		return null;
	}
}
