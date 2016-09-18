package net.openvoxel.networking.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.compression.Snappy;

import java.util.List;

/**
 * Created by James on 01/09/2016.
 */
public class CompressionCodec extends ByteToMessageCodec<ByteBuf>{

	private Snappy snappy;
	private static final int COMPRESSION_LIMIT = 24;// TODO: 01/09/2016 Tune This Value 

	public CompressionCodec() {
		snappy = new Snappy();
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
		final int len = msg.readableBytes();
		if(len > COMPRESSION_LIMIT) {
			out.writeByte(1);
			snappy.encode(msg,out,len);
		}else{
			out.writeByte(0);
			out.writeBytes(msg);
		}
		msg.release();//Memory Cleanup//
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		final boolean isCompressed = in.readByte() == 1;
		if(isCompressed) {//Decompress
			ByteBuf buff = ctx.alloc().buffer();
			snappy.decode(in,buff);
			out.add(buff);
		}else{
			out.add(in);//Reader Index Moved//
		}
	}
}
