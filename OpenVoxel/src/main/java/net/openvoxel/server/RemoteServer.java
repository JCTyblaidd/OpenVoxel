package net.openvoxel.server;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.common.entity.living.player.EntityPlayer;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;
import net.openvoxel.files.GameSave;
import net.openvoxel.networking.ServerNetworkHandler;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.utility.Command;
import net.openvoxel.utility.CrashReport;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by James on 25/08/2016.
 *
 * Server Instance Hosted By Dedicated Servers
 *
 * Automatically Loaded / Creates Server Data File
 */
public class RemoteServer extends Server {

	protected GameSave savedInfo;
	protected TIntObjectMap<World> dimensionMap;
	protected ServerNetworkHandler networkHandler;

	private Set<EntityPlayer> connectedPlayers;

	public RemoteServer(GameSave saveData) {
		savedInfo = saveData;
		dimensionMap = new TIntObjectHashMap<>();
		networkHandler = new ServerNetworkHandler(this);
		connectedPlayers = new HashSet<>();
	}

	@Override
	public void host(int PORT) {
		try {
			networkHandler.Host(PORT);
			Logger.getLogger("Server").Info("Server Hosted on localhost:"+PORT);
		}catch (IOException except) {
			CrashReport crashReport = new CrashReport("Error Hosting");
			OpenVoxel.reportCrash(crashReport);
		}
	}

	@Override
	public World getWorldAtDimension(int dimensionID) {
		return dimensionMap.get(dimensionID);
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public void callCommand(Command command) {
		//TODO: implement
	}

	@Override
	public void gameLogicTick() {
		//TODO: implement & async per dimension???
	}

	@Override
	public SimpleChannelInboundHandler<AbstractPacket> create() {
		return new ServerPlayerConnection(this);
	}

	/////////////////////////////////////////
	/// Unsupported ClientSide Operations ///
	/////////////////////////////////////////

	@Override
	public World getMyWorld() {
		throw new UnsupportedOperationException("Remote Server doesn't have a player!");
	}

	@Override
	public EntityPlayerSP getMyPlayer() {
		throw new UnsupportedOperationException("Remote Server doesn't have a player!");
	}
}
