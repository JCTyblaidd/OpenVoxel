package net.openvoxel.server;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.api.util.PerSecondTimer;
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

	protected PerSecondTimer tickRateTimer = new PerSecondTimer();

	private long lastTickTime = System.currentTimeMillis();
	private static final int TICK_MILLIS = 50;

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

	public float getTickRate() {
		return tickRateTimer.getPerSecond();
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


	protected void executeServerTick(AsyncBarrier barrier) {
		tickRateTimer.notifyEvent();
	}

	public final void serverTick(AsyncBarrier barrier) {
		long currentTick = System.currentTimeMillis();
		int deltaTick = (int)(currentTick-lastTickTime);
		if(deltaTick > TICK_MILLIS) {
			lastTickTime = currentTick;
			executeServerTick(barrier);
		}else{
			barrier.reset(0);
		}
	}

	public void sendUpdates() {

	}
}
