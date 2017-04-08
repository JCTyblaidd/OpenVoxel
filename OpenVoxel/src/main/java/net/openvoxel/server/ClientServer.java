package net.openvoxel.server;

import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.utility.Command;

import java.net.SocketAddress;

/**
 * Created by James on 25/08/2016.
 *
 * Reference to a server hosted in another location(normally dedicated)
 */
public class ClientServer extends Server{

	private EntityPlayerSP currentPlayer;

	public ClientServer(SocketAddress address) {
		
	}

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public void callCommand(Command command) {

	}

	@Override
	public World getMyWorld() {
		return currentPlayer.currentWorld;
	}

	@Override
	public EntityPlayerSP getMyPlayer() {
		return currentPlayer;
	}

	@Override
	public void gameLogicTick() {
		//TODO: implement lock tick
	}

	@Override
	public SimpleChannelInboundHandler<AbstractPacket> create() {
		throw new UnsupportedOperationException("Client Mirror cannot host a connection");
	}

	@Override
	public void host(int PORT) {
		throw new UnsupportedOperationException("Client Mirror Of Server CANNOT Host");
	}

	@Override
	public World getWorldAtDimension(int dimensionID) {
		throw new UnsupportedOperationException("Not Implemented");
	}

}
