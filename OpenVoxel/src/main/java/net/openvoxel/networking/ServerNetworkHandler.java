package net.openvoxel.networking;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.openvoxel.OpenVoxel;
import net.openvoxel.networking.protocol.PacketChannelInitializer;
import net.openvoxel.server.Server;

/**
 * Created by James on 25/08/2016.
 *
 * Handles Server Network Connections
 */
public class ServerNetworkHandler extends NetworkHandler{

	private Server server;
	private ServerBootstrap NETTY;

	public ServerNetworkHandler(Server server) {
		this.server = server;
		NETTY = new ServerBootstrap();
		NETTY.channel(NioServerSocketChannel.class);
		NETTY.group(controlGroup,workerGroup);
		NETTY.childHandler(new PacketChannelInitializer(OpenVoxel.getInstance().packetRegistry,null));
	}


	public void Host(int port) {

	}

}
