package net.openvoxel.server;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.networking.ClientNetworkHandler;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.server.util.ClientChunkLoadManager;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.world.client.ClientWorld;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * Created by James on 09/04/2017.
 *
 * Client Side Server Code
 */
public class ClientServer extends BaseServer implements Consumer<AbstractPacket> {

	protected EntityPlayerSP thePlayer;

	protected ClientNetworkHandler serverConnection;

	public ClientChunkLoadManager loadManager;

	public ClientServer() {

	}

	public void connectTo(SocketAddress address) throws IOException {
		serverConnection.connectTo(address);
	}

	public void connectToLocal() {
		try {
			serverConnection.connectToLocal();
		}catch (Exception ex) {
			CrashReport crashReport = new CrashReport("Error Connecting to Local Server");
			crashReport.caughtException(ex);
			OpenVoxel.reportCrash(crashReport);
		}
	}


	@Override
	public void run() {
		serverConnection.handleAllRecievedPackets(this);
		//simulate client side//

		//Await Timeout//
	}

	public EntityPlayerSP getThePlayer() {
		return thePlayer;
	}

	public ClientWorld getTheWorld() {
		return (ClientWorld)thePlayer.currentWorld;
	}

	@Override
	public void start() {
		super.start();
	}

	public void disconnect() {
		shutdown();
		//todo: send exit packet;
		serverConnection.shutdown();
	}

	@Override
	public void accept(AbstractPacket abstractPacket) {
		Logger.INSTANCE.Info("Packet:" + abstractPacket.getClass().getSimpleName());
	}
}
