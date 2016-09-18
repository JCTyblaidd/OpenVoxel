package net.openvoxel.server;

import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.ChunkCoordinate;
import net.openvoxel.common.world.World;
import net.openvoxel.common.world.generation.DebugWorldGenerator;

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

	public LocalServer() {
		//DEBUG CODE//
		thePlayer = new EntityPlayerSP();
		this.dimensionMap.put(0,new World(new DebugWorldGenerator()));
		this.dimensionMap.get(0).loadChunk(new ChunkCoordinate(0,0));
		this.dimensionMap.get(0).addEntityToWorld(thePlayer);
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public World getMyWorld() {
		return dimensionMap.get(0);
	}

	@Override
	public EntityPlayerSP getMyPlayer() {
		return thePlayer;
	}

}
