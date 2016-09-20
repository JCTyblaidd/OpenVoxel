package net.openvoxel.server.dedicated;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by James on 25/08/2016.
 *
 * Thread to handle server console commands asynchronously
 */
public class CommandInputThread implements Runnable{

	public static CommandInputThread INSTANCE;
	public Thread thread;

	public static void Start() {
		INSTANCE = new CommandInputThread();
		INSTANCE.thread.start();
	}

	private CommandInputThread() {
		thread = new Thread(this,"Open Voxel: Server Command Input Thread");
	}

	@Override
	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (true) {
				String CMD = reader.readLine();
				handleCmd(CMD);
			}
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleCmd(String cmd) {
		// TODO: 25/08/2016 Push to a command handler
	}
}
