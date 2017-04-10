package net.openvoxel.server.util;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;

/**
 * Created by James on 09/04/2017.
 *
 * Handles the loading and unloading of various chunks on the clients side
 */
@SideOnly(side = Side.CLIENT)
public class ClientChunkLoadManager {

	private int chunkLoadDistance;
	private boolean loadChunksSquare;

	public ClientChunkLoadManager() {
		chunkLoadDistance = 15;
		loadChunksSquare = true;
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

}
