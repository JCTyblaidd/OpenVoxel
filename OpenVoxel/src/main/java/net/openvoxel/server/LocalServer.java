package net.openvoxel.server;

import net.openvoxel.common.entity.living.player.EntityPlayerMP;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.ChunkCoordinate;
import net.openvoxel.common.world.World;
import net.openvoxel.common.world.generation.DebugWorldGenerator;
import net.openvoxel.files.GameSave;
import net.openvoxel.networking.ClientNetworkHandler;

/**
 * Created by James on 25/08/2016.
 *
 * Reference to a server hosted on this computer, with all tweaks that apply, (acts both as a client and remote server with some optimizations for the local player)
 *
 * Tweaked Version of remote server
 *
 * Current State: Testing
 */
public class LocalServer extends RemoteServer {

	public EntityPlayerSP thePlayer;
	private boolean allowOtherConnections;
	private ClientNetworkHandler handler;

	public LocalServer(GameSave save, boolean allowOtherConnections) {
		super(save);
		this.allowOtherConnections = allowOtherConnections;

		//DEBUG CODE// TODO: remove and update
		thePlayer = new EntityPlayerSP();
		this.dimensionMap.put(0,new World(new DebugWorldGenerator()));
		for(int x = -10; x <= 10; x++) {
			for(int z = -10; z <= 10; z++) {
				this.dimensionMap.get(0).loadChunk(new ChunkCoordinate(x,z));
			}
		}
		this.dimensionMap.get(0).addEntityToWorld(thePlayer);

	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public World getMyWorld() {
		return thePlayer.currentWorld;
	}

	@Override
	public EntityPlayerSP getMyPlayer() {
		return thePlayer;
	}

}
