package net.openvoxel.server;

import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;

/**
 * Created by James on 25/08/2016.
 *
 * Reference to a server hosted in another location(normally dedicated)
 */
public class ClientServer extends Server{

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public World getMyWorld() {
		return null;
	}

	@Override
	public EntityPlayerSP getMyPlayer() {
		return null;
	}

	@Override
	public void host(int PORT) {
		throw new UnsupportedOperationException("Client Mirror Of Server CANNOT Host");
	}

}
