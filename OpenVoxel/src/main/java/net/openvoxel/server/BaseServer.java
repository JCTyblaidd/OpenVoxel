package net.openvoxel.server;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.common.entity.living.player.EntityPlayer;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.world.World;
import net.openvoxel.world.generation.DebugWorldGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 09/04/2017.
 *
 * Common Code Between The Client and Standard Server Information
 */
public abstract class BaseServer {

	protected TIntObjectMap<World> dimensionMap;

	protected List<EntityPlayer> connectedPlayers;

	public World loadDimension(int index) {
		World world = dimensionMap.get(index);
		if(world == null) {
			//TODO: USE PROPER DIMENSION REGISTRY
			world = new World(new DebugWorldGenerator());
			dimensionMap.put(index,world);
		}
		return world;
	}

	public void unloadDimension(int index) {
		World world = dimensionMap.get(index);
		if(world != null) {
			//TODO: UNLOAD WORLD
		}
		dimensionMap.remove(index);
	}

	///////////////
	// Main Code //
	///////////////

	BaseServer() {
		dimensionMap = new TIntObjectHashMap<>();
		connectedPlayers = new ArrayList<>();
	}

	public void startup() {

	}

	public void shutdown() {
		dimensionMap.forEachValue(world -> {
			world.releaseAllChunkData();
			return true;
		});
	}

	public void serverTick(AsyncBarrier barrier) {

	}

	public void sendUpdates() {

	}
}
