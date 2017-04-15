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
		chunkLoadDistance = 5;
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

	/**
	 * Limit the number of updates per tick
	 */
	private static final int requestLimit = 5;

	public void tick() {
		int requestCount = 0;
		EntityPlayerSP player = clientServer.getThePlayer();
		ClientWorld world = (ClientWorld)player.currentWorld;
		int playerChunkX = (int)(player.xPos / 16);
		int playerChunkZ = (int)(player.zPos / 16);

		int xMin = playerChunkX - chunkLoadDistance;
		int xMax = playerChunkX + chunkLoadDistance;
		int zMin = playerChunkZ - chunkLoadDistance;
		int zMax = playerChunkZ + chunkLoadDistance;
		//TODO: correct spiral implementation
		for(int dz = 0; dz <= chunkLoadDistance; dz++) {
			for(int dx = 0; dx <= chunkLoadDistance; dx++) {
				for(int sz = -1; sz <= 1; sz += 2) {
					for(int sx = -1; sx <= 1; sx += 2) {
						int x = playerChunkX + (dx * sx);
						int z = playerChunkZ + (dz * sz);
						ClientChunk value = loadedChunks.get(x,z);
						if(value == null) {
							clientServer.requestChunkLoad(world,x,z);
							requestCount++;
							if(requestCount >= requestLimit) return;
						}else if(value.requiresUpdate()) {
							value.markUpdated();
							clientServer.requestChunkUpdate(world,x,z);
							requestCount++;
							if(requestCount >= requestLimit) return;
						}
					}
				}
			}
		}
		/**
		for(int z = zMin; z <= zMax; z++) {
			for(int x = xMin; x <= xMax; x++) {
				ClientChunk value = loadedChunks.get(x,z);
				if(value == null) {
					clientServer.requestChunkLoad(world,x,z);
					requestCount++;
					if(requestCount >= requestLimit) return;
				}else if(value.requiresUpdate()) {
					value.markUpdated();
					clientServer.requestChunkUpdate(world,x,z);
					requestCount++;
					if(requestCount >= requestLimit) return;
				}
			}
		}**/
		List<ClientChunk> toUnloadChunks = new ArrayList<>();
		loadedChunks.forEachChunkCoord((x,z,chunk) -> {
			if(x < xMin || x > xMax || z < zMin || z > zMax) {
				toUnloadChunks.add(chunk);
			}
		});
		while (toUnloadChunks.size() > requestLimit) {
			toUnloadChunks.remove(toUnloadChunks.size() - 1);
		}
		toUnloadChunks.forEach(chunk -> {
			clientServer.requestChunkUnload(world,chunk.chunkX,chunk.chunkZ);
			loadedChunks.remove(chunk.chunkX,chunk.chunkZ);
		});
	}

}
