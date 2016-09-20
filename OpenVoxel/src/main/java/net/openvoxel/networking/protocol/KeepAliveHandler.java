package net.openvoxel.networking.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import net.openvoxel.networking.packet.protocol.KeepAlivePacket;

import java.util.concurrent.TimeUnit;

/**
 * Created by James on 01/09/2016.
 *
 * Keeps a connection alive,
 *
 * after X seconds have passed since the last message was sent, send a keep alive message to prevent loss of connection
 */
public class KeepAliveHandler extends IdleStateHandler {

	private KeepAlivePacket KEEP_ALIVE;

	public KeepAliveHandler(int numSeconds) {
		super(0, numSeconds, 0, TimeUnit.SECONDS);
		KEEP_ALIVE = new KeepAlivePacket();
	}

	@Override
	protected final void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
		assert( evt.state() == IdleState.WRITER_IDLE );
		writeTimedOut(ctx);
	}

	protected void writeTimedOut(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(KEEP_ALIVE);
	}
}
