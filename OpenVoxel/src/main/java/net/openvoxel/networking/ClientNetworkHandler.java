package net.openvoxel.networking;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.openvoxel.OpenVoxel;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.PacketChannelInitializer;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Created by James on 25/08/2016.
 *
 */
public class ClientNetworkHandler extends NetworkHandler {

	Bootstrap NETTY;
	ClientMessageHandler clientMessageHandle;
	Deque<AbstractPacket> packetQueue = new ArrayDeque<>();
	private Channel rawConnection;

	public ClientNetworkHandler() {
		NETTY = new Bootstrap();
		NETTY.channel(NioSocketChannel.class);
		NETTY.group(workerGroup);
		clientMessageHandle = new ClientMessageHandler(this);
		NETTY.handler(new PacketChannelInitializer(OpenVoxel.getInstance().packetRegistry,() -> clientMessageHandle));
	}

	public void sendJoinServerRequest() {

	}

	public void connectTo(SocketAddress address) throws IOException {
		try {
			rawConnection = NETTY.connect(address).sync().channel();
			sendJoinServerRequest();
		}catch (InterruptedException ex) {
			throw new IOException("Failed to connect to address");
		}
	}

	public void connectToLocal() throws IOException{
		connectTo(new LocalAddress("localhost:2500"));
		sendJoinServerRequest();
	}

	public boolean isConnected() {
		return rawConnection != null;
	}

	public void sendPacket(AbstractPacket packet) {
		rawConnection.writeAndFlush(packet);
	}

	public synchronized void handleAllRecievedPackets(Consumer<AbstractPacket> handler) {
		while(!packetQueue.isEmpty()) {
			handler.accept(packetQueue.removeLast());
		}
	}

	public void handlePacketsWithTimeout(int millis) {

	}

	public void shutdown() {}


	public static class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractPacket> {
		final ClientNetworkHandler clientNetworkHandler;
		private ClientMessageHandler(ClientNetworkHandler networkHandler) {
			clientNetworkHandler = networkHandler;
		}
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, AbstractPacket msg) throws Exception {
			synchronized (clientNetworkHandler) {
				clientNetworkHandler.packetQueue.addFirst(msg);
			}
		}
	}
}
