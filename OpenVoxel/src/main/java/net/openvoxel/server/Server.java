package net.openvoxel.server;

import com.jc.util.stream.utils.Producer;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;
import net.openvoxel.files.GameSave;
import net.openvoxel.networking.ServerNetworkHandler;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.utility.Command;
import net.openvoxel.utility.CrashReport;

import java.io.IOException;

/**
 * Created by James on 25/08/2016.
 *
 * Base Server Class
 */
public abstract class Server implements Producer<SimpleChannelInboundHandler<AbstractPacket>> {

	public abstract void host(int PORT);

	public Server() {
		OpenVoxel.getInstance().packetRegistry.generateDefaultMappings();
	}

	public abstract World getWorldAtDimension(int dimensionID);

	public abstract boolean isRemote();

	public abstract void callCommand(Command command);

	//Client Side Only, get the world I Am In
	@SideOnly(side= Side.CLIENT)
	public abstract World getMyWorld();

	@SideOnly(side = Side.CLIENT)
	public abstract EntityPlayerSP getMyPlayer();

	/**Only To Be Called From The Game Logic Thread*/
	public abstract void gameLogicTick();

	@Override
	public abstract SimpleChannelInboundHandler<AbstractPacket> create();
}
