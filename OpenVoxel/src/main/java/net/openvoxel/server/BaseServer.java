package net.openvoxel.server;

/**
 * Created by James on 09/04/2017.
 *
 * Common Code Between The Client and Standard Server Information
 */
public abstract class BaseServer implements Runnable{

	/**
	 * Synchronised(Concurrent Safe) Map
	 */
	/**
	TIntObjectMap<World> dimensionMap;

	private GameTickThread gameTickThread;


	protected List<EntityPlayer> connectedPlayers;
	**/
	BaseServer() {
		//gameTickThread = new GameTickThread(this,this.toString(),this::onCleanup);
		//dimensionMap = new TSynchronizedIntObjectMap<>(new TIntObjectHashMap<>());
		//connectedPlayers = new ArrayList<>();
	}

	void start() {
		//gameTickThread.start();
	}

	void shutdown() {
		//gameTickThread.terminate();
	}

	//void onCleanup() {
		//TODO: save data on shutdown
		//dimensionMap.forEachValue(e -> {
		//	e.releaseAllChunkData();
		//	return true;
		//});
		//dimensionMap.clear();
		//connectedPlayers.clear();
	//}

	//@Override
	//public String toString() {
	//	return getClass().getSimpleName() + ": [Unknown]";
	//}
}
