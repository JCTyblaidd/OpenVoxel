package net.openvoxel;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.api.logger.GLFWLogWrapper;
import net.openvoxel.api.logger.LWJGLLogWrapper;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.logger.NettyLogWrapper;
import net.openvoxel.api.login.UserData;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.util.Version;
import net.openvoxel.client.audio.ClientAudio;
import net.openvoxel.client.control.RenderThread;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.renderer.generic.DisplayHandle;
import net.openvoxel.common.GameThread;
import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.common.event.EventBus;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.window.ProgramShutdownEvent;
import net.openvoxel.common.event.window.WindowCloseRequestedEvent;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.registry.RegistryEntities;
import net.openvoxel.common.registry.RegistryItems;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.networking.protocol.PacketRegistry;
import net.openvoxel.server.RemoteServer;
import net.openvoxel.server.Server;
import net.openvoxel.server.dedicated.CommandInputThread;
import net.openvoxel.utility.CrashReport;

import java.net.SocketAddress;

/**
 * Created by James on 25/08/2016.
 *
 * Main Launch Class
 */
@SuppressWarnings("unused")
public class OpenVoxel implements EventListener{

	public static final Version currentVersion = new Version(0,0,1);

	public volatile boolean isRunning = true;
	public EventBus eventBus;

	private static ArgumentParser args;
	private static OpenVoxel instance;

	public Server currentServer = null;
	public PacketRegistry packetRegistry = PacketRegistry.CreateWithDefaults();
	public RegistryBlocks blockRegistry;
	public RegistryItems itemRegistry;
	public RegistryEntities entityRegistry;

	public UserData userData;

	public void HostServer(Server server) {
		if(currentServer != null) {
			//Kill// // TODO: 04/09/2016
		}
		currentServer = server;
		currentServer.loadDataMappings();
	}

	//Initialize Client Connections//
	public void clientConnectToLocalHost() {
		startServerConnectionBootstrap();
	}
	//Initialize Client Connections -> Remove//
	public void clientConnectRemote(SocketAddress address) {

		startServerConnectionBootstrap();
	}
	private void startServerConnectionBootstrap() {

	}

	public static OpenVoxel getInstance() {
		return instance;
	}

	public static ArgumentParser getLaunchParameters() {
		return args;
	}

	public static Server getServer() {
		return instance.currentServer;
	}

	public static void pushEvent(AbstractEvent event) {
		instance.eventBus.push(event);
	}

	public static void registerEvents(EventListener l) {
		instance.eventBus.register(l);
	}

	public static void reportCrash(CrashReport report) {
		//TODO: implement
	}

	/**MAIN CLASS ENTRY POINT**/
	public OpenVoxel(String[] arguments,String[] modClasses,String[] asmHandles,boolean isClient) {
		instance = this;
		args = new ArgumentParser(arguments);
		Side.isClient = isClient;
		eventBus = new EventBus();
		eventBus.register(this);


		//Configure Debug Settings//
		if(args.hasFlag("noChecks")) {//Tiny Performance Improvement
			System.setProperty("org.lwjgl.glfw.checkThread0","false");
			System.setProperty("org.lwjgl.util.NoChecks","true");
		}else if(args.hasFlag("debugChecks")) {//For Debugging//
			System.setProperty("org.lwjgl.util.Debug","true");
		}
		Logger.getLogger("Open Voxel").Info("Loaded Open Voxel "+currentVersion.getValString());

		//Hook Debug Output//
		if(args.hasFlag("bonusLogging")) {
			NettyLogWrapper.Load();
			if (isClient) {
				LWJGLLogWrapper.Load();
				GLFWLogWrapper.Load();
			}
		}
		//Acquire user data if ClientSide
		if(isClient) {
			userData = UserData.from(args);
		}

		//Wrap Mod Launching//
		ModLoader.Initialize(modClasses);
		ModLoader.getInstance().asmClasses = asmHandles;// TODO: 25/08/2016 Store ASM Classes Better
		blockRegistry = new RegistryBlocks();
		itemRegistry = new RegistryItems(blockRegistry);
		entityRegistry = new RegistryEntities();
		//Bootstrap Threading///
		if(isClient) {
			Renderer.Initialize();
			RenderThread.Start();
			ClientAudio.Load();
		}else {
			CommandInputThread.Start();
		}
		GameThread.Start();
		if(!isClient) {
			GameThread.INSTANCE.awaitModsLoaded();
			Logger.getLogger("Dedicated Server").Info("Starting Server....");
			HostServer(new RemoteServer());
		}
		//Convert Bootstrap Thread Into InputPollThread if clientSide ELSE end the thread//
		if(isClient) {
			DisplayHandle handle = Renderer.renderer.getDisplayHandle();
			while(isRunning) {
				//Poll Inputs : setupLikeThis Due to GLFW needing event polls from the main thread//
				handle.pollEvents();
				try {
					Thread.sleep(16);//RoundDown from 1000 / 60 (60 Polls Per Second)
				} catch (InterruptedException e) {}
			}
		}
	}

	public void AttemptShutdownSequence(boolean isCrash) {
		WindowCloseRequestedEvent e1 = new WindowCloseRequestedEvent();
		eventBus.push(e1);
		if(!e1.isCancelled()) {
			//notifyEvent Main Loops Of Shutdown//
			isRunning = false;
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e) {}
			//Push Event After Timeout//
			Logger.getLogger("OpenVoxel").Info("Shutting Down...");
			ProgramShutdownEvent e2 = new ProgramShutdownEvent(isCrash);
			eventBus.push(e2);
			System.exit(isCrash ? -1 : 0);
		}
	}
}
