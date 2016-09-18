package net.openvoxel.networking.poll;

import net.openvoxel.api.logger.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by James on 04/09/2016.
 */
public class UDPServerPollLoop implements Runnable{

	public static UDPServerPollLoop INSTANCE;
	public Thread thread;
	public boolean isRunning = false;
	public int port = 6556;

	public UDPServerPollLoop() {
		thread = new Thread(this,"Open Voxel: Server UDP Poll Thread");
	}

	private static UDPServerPollLoop inst() {
		if(INSTANCE == null) {
			INSTANCE = new UDPServerPollLoop();
		}
		return INSTANCE;
	}

	public static void Start(int port) {
		if(inst().isRunning) {
			Stop();
		}
		inst().port = port;
		while(inst().thread.isAlive()) {
			try {
				Thread.sleep(10);
			}catch(Exception e) {}
		}
		inst().isRunning = true;
		inst().thread.start();
	}
	public static void Stop() {
		inst().isRunning = false;
		inst().thread.interrupt();
	}

	@Override
	public void run() {
		try {
			DatagramSocket socket = new DatagramSocket(port);
			byte[] buffer = new byte[64];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			byte[] buffer2 = new byte[256];
			DatagramPacket response = new DatagramPacket(buffer2,buffer2.length);
			while(isRunning) {
				socket.receive(packet);
				response.setSocketAddress(packet.getSocketAddress());//Reply//
				socket.send(response);
			}
		}catch(Exception e) {
			Logger.getLogger("Server UDP Info Loop").Severe("An Error Occurred : Stopping");
			Logger.INSTANCE.StackTrace(e);
		}
	}
}
