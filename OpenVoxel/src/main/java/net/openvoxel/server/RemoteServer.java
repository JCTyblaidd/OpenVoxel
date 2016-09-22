package net.openvoxel.server;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;

/**
 * Created by James on 25/08/2016.
 *
 * Server Instance Hosted By Dedicated Servers
 *
 * Automatically Loaded / Creates Server Data File
 */
public class RemoteServer extends Server{

	public RemoteServer() {
		Logger.INSTANCE.Info("Server Hosting");

	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public World getMyWorld() {
		throw new UnsupportedOperationException("Remote Server doesn't have a player!");
	}

	@Override
	public EntityPlayerSP getMyPlayer() {
		throw new UnsupportedOperationException("Remote Server doesn't have a player!");
	}
}
