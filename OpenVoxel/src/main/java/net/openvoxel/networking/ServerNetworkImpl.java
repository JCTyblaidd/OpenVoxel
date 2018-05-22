package net.openvoxel.networking;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.openvoxel.common.entity.living.player.EntityPlayer;
import net.openvoxel.networking.protocol.AbstractPacket;

import java.io.Closeable;
import java.util.function.BiConsumer;

/**
 * Network manager for non remote-servers
 */
public class ServerNetworkImpl implements Closeable {

	private NioEventLoopGroup workerGroup;
	private NioEventLoopGroup controlGroup;
	private ServerBootstrap serverBootstrap;

	private boolean enableRemote;
	private BiConsumer<EntityPlayer,AbstractPacket> packetHandler;

	public ServerNetworkImpl(BiConsumer<EntityPlayer,AbstractPacket> handler) {
		enableRemote = false;
		packetHandler = handler;
		workerGroup = new NioEventLoopGroup();
		controlGroup = new NioEventLoopGroup();
		serverBootstrap = new ServerBootstrap();
		serverBootstrap.channel(NioServerSocketChannel.class);
		serverBootstrap.group(controlGroup,workerGroup);
	}

	public boolean getAllowRemote() {
		return enableRemote;
	}

	public void setAllowRemote(boolean allowRemote) {
		enableRemote = allowRemote;
	}

	@Override
	public void close() {
		try{
			//TODO: IMP
		}finally {
			workerGroup.shutdownGracefully();
			controlGroup.shutdownGracefully();
		}
	}

	//////////////////////////////
	/// Private Implementation ///
	//////////////////////////////

}
