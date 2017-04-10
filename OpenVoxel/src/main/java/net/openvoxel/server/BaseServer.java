package net.openvoxel.server;

import gnu.trove.impl.sync.TSynchronizedIntObjectMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.common.GameTickThread;
import net.openvoxel.common.entity.living.player.EntityPlayer;
import net.openvoxel.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 09/04/2017.
 *
 * Common Code Between The Client and Standard Server Information
 */
abstract class BaseServer implements Runnable{

	/**
	 * Synchronised(Concurrent Safe) Map
	 */
	TIntObjectMap<World> dimensionMap;

	private GameTickThread gameTickThread;


	protected List<EntityPlayer> connectedPlayers;

	BaseServer() {
		gameTickThread = new GameTickThread(this,this.toString());
		dimensionMap = new TSynchronizedIntObjectMap<>(new TIntObjectHashMap<>());
		connectedPlayers = new ArrayList<>();
	}

	void start() {
		gameTickThread.start();
	}

	void shutdown() {
		gameTickThread.terminate();
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": [Unknown]";
	}
}
