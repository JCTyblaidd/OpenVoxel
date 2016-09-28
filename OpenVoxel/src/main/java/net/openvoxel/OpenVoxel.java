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

	/**
	 * The current Version of the Game
	 */
	public static final Version currentVersion = new Version(0,0,1);

	/**
	 * Is the game currently Running
	 */
	public volatile boolean isRunning = true;
	/**
	 * The Global EventBus
	 */
	public final EventBus eventBus;

	/**
	 * Parsed Reference to the main arguments
	 */
	private static ArgumentParser args;
	/**
	 * The Main Instance of the Game Class
	 */
	private static OpenVoxel instance;

	public Server currentServer = null;
	/**
	 * The ID <-> Name <-> Packet Registry,
	 * Automatically Synchronised on connection to a (non-local) server
	 */
	public PacketRegistry packetRegistry = PacketRegistry.CreateWithDefaults();
	/**
	 * The ID <-> Name <-> Block Instance Registry
	 * Automatically Synchronised on connection to a (non-local) server
	 */
	public RegistryBlocks blockRegistry;
	/**
	 * The ID <-> Name <-> Item Instance Registry
	 * Automatically Synchronised on connection to a (non-local) server
	 */
	public RegistryItems itemRegistry;
	/**
	 * The Name <-> Entity Class Registry
	 * No Synchronisation Required
	 */
	public RegistryEntities entityRegistry;

	/**
	 * Information about the current User: Currently Unused
	 */
	public UserData userData;

	/**
	 * Tells the game to bootstrap a server instance
	 * Local Server => Host + Connect
	 * Remote Server => Host
	 * Client Server => Connect
	 * @param server
	 */
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

	/**
	 * @return the current instance
	 */
	public static OpenVoxel getInstance() {
		return instance;
	}

	/**
	 * @return the argument parser
	 */
	public static ArgumentParser getLaunchParameters() {
		return args;
	}

	/**
	 * @return the current server, NULL if it doesn't exist
	 */
	public static Server getServer() {
		return instance.currentServer;
	}

	/**
	 * @param event call an event through the eventBus
	 */
	public static void pushEvent(AbstractEvent event) {
		instance.eventBus.push(event);
	}

	/**
	 * @param listener The Event Listener to register for events
	 */
	public static void registerEvents(EventListener listener) {
		instance.eventBus.register(listener);
	}

	/**
	 * @param report a crash
	 */
	public static void reportCrash(CrashReport report) {
		report.printStackTrace();
		instance.AttemptShutdownSequence(true);
	}

	/**
	 * Main OpenVoxel ClassLoader Entry Location
	 * @param arguments the main class args variable
	 * @param modClasses the class names of the mod classes to load
	 * @param asmHandles the class names of asm handlers as reference
	 * @param isClient are we launching as the client? Yes / No
	 */
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
				} catch (InterruptedException e) {
					//Thread Interrupted
					Logger.getLogger("Poll Thread").Warning("Thread Interrupted");
				}
			}
		}
	}

	/**
	* Start the Shutdown Sequence, Can be cancelled
	* @param isCrash is the shutdown unexpected
	**/
	public void AttemptShutdownSequence(boolean isCrash) {
		WindowCloseRequestedEvent e1 = new WindowCloseRequestedEvent(isCrash);
		eventBus.push(e1);
		if(!e1.isCancelled() || isCrash) {
			if(e1.isCancelled()) {
				Logger.getLogger("OpenVoxel").Warning("Window Close Requested Event for Crash Stopped, Invalid Behaviour");
			}
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
