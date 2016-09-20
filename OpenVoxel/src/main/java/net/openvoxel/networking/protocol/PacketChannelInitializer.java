package net.openvoxel.networking.protocol;

import com.jc.util.stream.utils.Producer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Created by James on 01/09/2016.
 *
 * Channel Pipeline Initializer
 */
public class PacketChannelInitializer extends ChannelInitializer<SocketChannel>{

	private PacketRegistry registry;
	private Producer<SimpleChannelInboundHandler<AbstractPacket>> handler;

	public PacketChannelInitializer(PacketRegistry registry, Producer<SimpleChannelInboundHandler<AbstractPacket>> handler) {
		this.registry = registry;
		this.handler = handler;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		//Packet <-> Byte//
		pipeline.addLast(new CompressionCodec());
		pipeline.addLast(new PacketCodec(registry));

		//State Handler//
		pipeline.addLast(new ReadTimeoutHandler(20));//20 second timeout//
		pipeline.addLast(new KeepAliveHandler(5));//if not traffic sent for 5 seconds -> send a keep alive packet//

		//Inbound Handler//
		pipeline.addLast(handler.create());
	}
}
