package net.openvoxel.networking;

import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Created by James on 01/09/2016.
 *
 * Cache for the Netty Network Handlers
 *
 */
abstract class NetworkHandler {

	static NioEventLoopGroup workerGroup;
	static NioEventLoopGroup controlGroup;
	static {
		workerGroup = new NioEventLoopGroup();
		controlGroup = new NioEventLoopGroup();
	}

}
