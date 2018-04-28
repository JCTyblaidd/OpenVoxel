package net.openvoxel.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.login.UserData;
import net.openvoxel.common.entity.living.player.EntityPlayerMP;
import net.openvoxel.networking.packet.protocol.HandshakePacket;
import net.openvoxel.networking.packet.protocol.JoinGamePacket;
import net.openvoxel.networking.packet.protocol.RequestPacketSync;
import net.openvoxel.networking.packet.sync.RequestServerSyncPacket;
import net.openvoxel.networking.packet.sync.SyncRegistryPacket;
import net.openvoxel.networking.protocol.AbstractPacket;

/**
 * Created by James on 25/09/2016.
 *
 * Server Player Connection
 */
public class ServerPlayerConnection extends SimpleChannelInboundHandler<AbstractPacket> {
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, AbstractPacket abstractPacket) {

	}

	/**
	private Server server;
	private EntityPlayerMP playerMP;
	private UserData userData;
	private ChannelHandlerContext context;

	ServerPlayerConnection(Server server) {
		this.server = server;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if(server instanceof LocalServer) return;//Local Server is automatically authenticated//
		ctx.pipeline().addBefore("exec_handler", "auth_handler", new SimpleChannelInboundHandler<AbstractPacket>() {
			private boolean hasHandshake = false;
			private boolean hasPacketSynced = false;
			private boolean syncedMods = false;
			private boolean syncedRegistry = false;
			@Override
			protected void channelRead0(ChannelHandlerContext ctx, AbstractPacket msg) throws Exception {
				if(msg instanceof HandshakePacket) {
					if(((HandshakePacket) msg).directedToServer){
						hasHandshake = true;
						ctx.writeAndFlush(new HandshakePacket(false));
					}else{
						ctx.close();
					}
				}else if (msg instanceof RequestPacketSync && hasHandshake) {
					ctx.writeAndFlush(OpenVoxel.getInstance().packetRegistry.createLoadPacketRegistryPacket(false));
					hasPacketSynced = true;
				}else if(msg instanceof RequestServerSyncPacket && hasPacketSynced) {
					if(((RequestServerSyncPacket) msg).syncID == 0) {
						//TODO:
						syncedMods = true;
						SyncRegistryPacket packet = new SyncRegistryPacket();
						ctx.write(packet);//TODO: add information
					}else{
						//Block And Item Sync//
						syncedRegistry = true;
					}
				}else if(msg instanceof JoinGamePacket && syncedMods && syncedRegistry) {
					userData = ((JoinGamePacket) msg).getUserData();
					ctx.pipeline().remove(this);
				}else{
					Logger.getLogger("Connection with invalid handshake");
					ctx.close();
				}
			}
		});
		context = ctx;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, AbstractPacket msg) throws Exception {

	}

	public void sendPacket(AbstractPacket pkt) {
		context.writeAndFlush(pkt);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		//Shutdown Everything//
		
	}**/
}
