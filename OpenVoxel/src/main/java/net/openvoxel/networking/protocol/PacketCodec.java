package net.openvoxel.networking.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * Created by James on 01/09/2016.
 */
public class PacketCodec extends ByteToMessageCodec<AbstractPacket>{

	public PacketRegistry packetRegistry;

	//TODO: test async//
	public ReadOnlyBuffer readOnlyBuffer;
	public WriteOnlyBuffer writeOnlyBuffer;


	public PacketCodec(PacketRegistry registry) {
		packetRegistry = registry;
		readOnlyBuffer = new ReadOnlyBuffer();
		writeOnlyBuffer = new WriteOnlyBuffer();
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, AbstractPacket msg, ByteBuf out) throws Exception {
		int ID = packetRegistry.getIDFromObject(msg);
		out.writeInt(ID);
		msg.storeData(writeOnlyBuffer.setBuffer(out));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int ID = in.readInt();
		AbstractPacket pkt = packetRegistry.getNewObject(ID);
		pkt.loadData(readOnlyBuffer.setBuffer(in));
		out.add(pkt);
	}

}
