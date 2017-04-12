package net.openvoxel.server.util;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.collection.ChunkMap;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientWorld;

import java.util.*;

/**
 * Created by James on 09/04/2017.
 *
 * Handles the loading and unloading of various chunks on the clients side
 */
@SideOnly(side = Side.CLIENT)
public class ClientChunkLoadManager {

	private int chunkLoadDistance;
	private boolean loadChunksSquare;
	private ClientServer clientServer;
	private ChunkMap<ClientChunk> loadedChunks;

	public ClientChunkLoadManager(ClientServer clientServer) {
		chunkLoadDistance = 8;
		loadChunksSquare = true;
		this.clientServer = clientServer;
		loadedChunks = new ChunkMap<>();
	}

	public void setLoadDistance(int val) {
		chunkLoadDistance = val;
	}

	public int getLoadDistance() {
		return chunkLoadDistance;
	}

	public void setChunkLoadStyle(boolean squareStyle) {
		loadChunksSquare = squareStyle;
	}

	public boolean getChunkLoadStyle() {
		return loadChunksSquare;
	}

	public void loadedChunk(ClientChunk chunk) {
		loadedChunks.set(chunk.chunkX,chunk.chunkZ,chunk);
	}

	public void tick() {
		EntityPlayerSP player = clientServer.getThePlayer();
		ClientWorld world = (ClientWorld)player.currentWorld;
		int playerChunkX = (int)(player.xPos / 16);
		int playerChunkZ = (int)(player.zPos / 16);
		int xMin = playerChunkX - chunkLoadDistance;
		int xMax = playerChunkX + chunkLoadDistance;
		int zMin = playerChunkZ - chunkLoadDistance;
		int zMax = playerChunkZ + chunkLoadDistance;
		for(int z = zMin; z <= zMax; z++) {
			for(int x = xMin; x <= xMax; x++) {
				ClientChunk value = loadedChunks.get(x,z);
				if(value == null) {
					clientServer.requestChunkLoad(world,x,z);
				}else if(value.requiresUpdate()) {
					value.markUpdated();
					clientServer.requestChunkUpdate(world,x,z);
				}
			}
		}
		//List<ClientChunk> toUnloadChunks = new ArrayList<>();
		//loadedChunks.forEachChunkCoord((x,z,chunk) -> {
		//	//TODO:
		//});
	}

}
