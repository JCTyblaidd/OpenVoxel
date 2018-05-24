package net.openvoxel.networking.network;

import net.openvoxel.networking.ClientNetwork;
import net.openvoxel.networking.ServerNetwork;
import net.openvoxel.networking.protocol.AbstractPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Consumer;

public class SinglePlayerNetwork implements ServerNetwork,ClientNetwork, ServerNetwork.ConnectedClient {

	private Deque<AbstractPacket> toClientQueue;
	private Deque<AbstractPacket> toServerQueue;

	public SinglePlayerNetwork(Consumer<ConnectedClient> consumer) {
		consumer.accept(this);
		toClientQueue = new ArrayDeque<>();
		toServerQueue = new ArrayDeque<>();
	}

	@Override
	public void close() {
		//NO OP
	}

	@Override
	public void forceDisconnect() {
		throw new UnsupportedOperationException("Cannot force disconnect local player");
	}

	@Override
	public boolean isLocalPlayer() {
		return true;
	}

	@Override
	public void sendPacketToClient(AbstractPacket packet) {
		toClientQueue.push(packet);
	}

	@Override
	public Iterator<AbstractPacket> getPacketsFromClient() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return !toClientQueue.isEmpty();
			}

			@Override
			public AbstractPacket next() {
				return toClientQueue.pop();
			}
		};
	}

	@Override
	public void sendPacketToServer(AbstractPacket packet) {
		toServerQueue.push(packet);
	}

	@Override
	public Iterator<AbstractPacket> getPacketsFromServer() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return !toServerQueue.isEmpty();
			}

			@Override
			public AbstractPacket next() {
				return toClientQueue.pop();
			}
		};
	}
}
