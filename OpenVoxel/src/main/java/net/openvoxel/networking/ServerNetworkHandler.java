package net.openvoxel.networking;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.openvoxel.OpenVoxel;
import net.openvoxel.networking.protocol.PacketChannelInitializer;
import net.openvoxel.server.DedicatedServer;

import java.io.IOException;

/**
 * Created by James on 25/08/2016.
 *
 * Handles Server Network Connections
 */
public class ServerNetworkHandler extends NetworkHandler{

	private DedicatedServer server;
	private ServerBootstrap NETTY;
	private ChannelFuture connectionFuture;

	public ServerNetworkHandler(DedicatedServer server) {
		this.server = server;
		NETTY = new ServerBootstrap();
		NETTY.channel(NioServerSocketChannel.class);
		NETTY.group(controlGroup,workerGroup);
		//NETTY.childHandler(new PacketChannelInitializer(OpenVoxel.getInstance().packetRegistry,this.server));
	}


	public void Host(int port) throws IOException {
		try {
			connectionFuture = NETTY.bind(port).sync();
		}catch (InterruptedException ex) {
			throw new IOException("Failed to Host Server");
		}
	}

	public void Shutdown() {
		try {
			connectionFuture.channel().close().sync();
		}catch (InterruptedException ex) {
			throw new RuntimeException("Failed to shutdown properly");
		} finally {
			controlGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}
