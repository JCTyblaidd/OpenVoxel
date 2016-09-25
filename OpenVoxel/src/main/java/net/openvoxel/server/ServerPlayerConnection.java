package net.openvoxel.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.networking.protocol.AbstractPacket;

/**
 * Created by James on 25/09/2016.
 *
 * Server Player Connection
 */
public class ServerPlayerConnection extends SimpleChannelInboundHandler<AbstractPacket>{
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, AbstractPacket msg) throws Exception {

	}
}
