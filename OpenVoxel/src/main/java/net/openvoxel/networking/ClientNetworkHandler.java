package net.openvoxel.networking;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.openvoxel.OpenVoxel;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.networking.protocol.PacketChannelInitialiser;
import net.openvoxel.server.LocalServer;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by James on 25/08/2016.
 *
 * TODO: rework network model
 */
public class ClientNetworkHandler extends NetworkHandler {

	OpenVoxel Handle;
	Bootstrap NETTY;
	ClientMessageHandler clientMessageHandle;
	Deque<AbstractPacket> packetQueue = new ArrayDeque<>();
	boolean isLocalServer = false;

	public ClientNetworkHandler(OpenVoxel openVoxel) {
		this.Handle = openVoxel;
		NETTY = new Bootstrap();
		NETTY.channel(NioSocketChannel.class);
		NETTY.group(workerGroup);
		clientMessageHandle = new ClientMessageHandler(this);
		NETTY.handler(new PacketChannelInitialiser(Handle.packetRegistry,() -> clientMessageHandle));//TODO: HANDLER
	}

	public void sendJoinServerRequest() {

	}

	public void connectTo(SocketAddress address) {
		isLocalServer = false;
	}

	public void connectToLocal() {
		LocalServer server = (LocalServer)OpenVoxel.getServer();
		isLocalServer = true;
	}

	public boolean isConnected() {
		return false;
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
