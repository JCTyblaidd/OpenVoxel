package net.openvoxel.server;

import com.jc.util.stream.utils.Producer;
import io.netty.channel.SimpleChannelInboundHandler;
import net.openvoxel.OpenVoxel;
import net.openvoxel.networking.ServerNetworkHandler;
import net.openvoxel.networking.protocol.AbstractPacket;
import net.openvoxel.utility.AsyncRunnablePool;
import net.openvoxel.utility.CrashReport;

import java.io.IOException;

/**
 * Created by James on 09/04/2017.
 *
 * Standard Server Base Class
 */
public class Server extends BaseServer implements Producer<SimpleChannelInboundHandler<AbstractPacket>> {

	private AsyncRunnablePool asyncExecutionService;
	private ServerNetworkHandler serverNetworkHandler;

	public Server() {
		asyncExecutionService = new AsyncRunnablePool("server_update",4);
		serverNetworkHandler = new ServerNetworkHandler(this);
	}

	public void start(int port) {
		super.start();
		try {
			serverNetworkHandler.Host(port);
		}catch(IOException ex) {
			CrashReport crashReport = new CrashReport("Error Starting Server").caughtException(ex);
			OpenVoxel.reportCrash(crashReport);
		}
	}

	public void shutdown() {
		super.shutdown();
		asyncExecutionService.stop();
	}

	@Override
	public SimpleChannelInboundHandler<AbstractPacket> create() {
		return new ServerPlayerConnection(this);
	}

	@Override
	public void run() {
		dimensionMap.forEachValue(world -> {
			asyncExecutionService.addWork(world::gameLogicTick);
			return true;
		});
	}
}
