package net.openvoxel.networking;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.PacketChannelInitializer;
import net.openvoxel.server.LocalServer;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by James on 25/08/2016.
 *
 */
public class ClientNetworkHandler extends NetworkHandler {

	Bootstrap NETTY;
	ClientMessageHandler clientMessageHandle;
	Deque<AbstractPacket> packetQueue = new ArrayDeque<>();
	boolean isLocalServer = false;
	Channel rawConnection;

	public ClientNetworkHandler() {
		NETTY = new Bootstrap();
		NETTY.channel(NioSocketChannel.class);
		NETTY.group(workerGroup);
		clientMessageHandle = new ClientMessageHandler(this);
		NETTY.handler(new PacketChannelInitializer(OpenVoxel.getInstance().packetRegistry,() -> clientMessageHandle));
	}

	public void sendJoinServerRequest() {
		Logger.INSTANCE.Debug("Sending Server Connection Request");
	}

	public void connectTo(SocketAddress address) throws IOException {
		isLocalServer = false;
		try {
			rawConnection = NETTY.connect(address).sync().channel();
		}catch (InterruptedException ex) {
			throw new IOException("Failed to connect to address");
		}
	}

	public void connectToLocal() {
		LocalServer server = (LocalServer)OpenVoxel.getServer();
		isLocalServer = true;

	}

	public boolean isConnected() {
		return rawConnection != null;
	}

	public void sendPacket(AbstractPacket packet) {
		if(isLocalServer) {

		}
	}

	public synchronized void handleAllRecievedPackets() {
		while(!packetQueue.isEmpty()) {
			handlePacket(packetQueue.removeLast());
		}
	}

	public void handlePacket(AbstractPacket pkt) {
		//TODO: handle?
	}

	public void shutdown() {}



	public static class ClientMessageHandler extends SimpleChannelInboundHandler<AbstractPacket> {

		ClientNetworkHandler clientNetworkHandler;

		public ClientMessageHandler(ClientNetworkHandler networkHandler) {
			clientNetworkHandler = networkHandler;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, AbstractPacket msg) throws Exception {
			synchronized (clientNetworkHandler) {
				clientNetworkHandler.packetQueue.addFirst(msg);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();//LOG X-TREME//
		}
	}
}
