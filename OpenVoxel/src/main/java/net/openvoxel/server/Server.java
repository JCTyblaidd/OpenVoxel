package net.openvoxel.server;

import com.jc.util.stream.utils.Producer;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.common.world.World;
import net.openvoxel.networking.ServerNetworkHandler;
import net.openvoxel.networking.protocol.AbstractPacket;

/**
 * Created by James on 25/08/2016.
 */
public abstract class Server implements Producer<SimpleChannelInboundHandler<AbstractPacket>> {

	public ServerNetworkHandler Network;

	public void host(int PORT) {
		if(Network == null) {
			Network = new ServerNetworkHandler(this);
		}
		Network.Host(PORT);
	}

	public TIntObjectHashMap<World> dimensionMap;

	public Server() {
		dimensionMap = new TIntObjectHashMap<>();
	}

	public World getWorldAtDimension(int dimensionID) {
		return dimensionMap.get(dimensionID);
	}

	public abstract boolean isRemote();

	//Client Side Only, get the world I Am In
	@SideOnly(side= Side.CLIENT)
	public abstract World getMyWorld();

	@SideOnly(side = Side.CLIENT)
	public abstract EntityPlayerSP getMyPlayer();

	/**Only To Be Called From The Game Logic Thread*/
	public void gameLogicTick() {
		dimensionMap.forEachValue(o -> {o.gameLogicTick(); return true;});
	}

	public void loadDataMappings() {
		// TODO: 04/09/2016 Make It So That World Config Settings Are Loaded From The File
	}


	@Override
	public SimpleChannelInboundHandler<AbstractPacket> create() {
		return null;
	}
}
