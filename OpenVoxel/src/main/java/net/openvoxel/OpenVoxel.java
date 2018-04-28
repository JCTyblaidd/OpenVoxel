package net.openvoxel;

import com.jc.util.reflection.Reflect;
import com.jc.util.utils.ArgumentParser;
import net.openvoxel.api.PublicAPI;
import net.openvoxel.api.logger.GLFWLogWrapper;
import net.openvoxel.api.logger.LWJGLLogWrapper;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.logger.NettyLogWrapper;
import net.openvoxel.api.login.UserData;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.api.util.Version;
import net.openvoxel.client.audio.ClientAudio;
import net.openvoxel.client.gui.menu.ScreenMainMenu;
import net.openvoxel.client.gui_framework.GUI;
import net.openvoxel.client.renderer.Renderer;
import net.openvoxel.common.GameLoaderThread;
import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.common.event.EventBus;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.window.ProgramShutdownEvent;
import net.openvoxel.common.event.window.WindowCloseRequestedEvent;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.registry.RegistryEntities;
import net.openvoxel.common.registry.RegistryItems;
import net.openvoxel.files.FolderUtils;
import net.openvoxel.files.GameSave;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.networking.protocol.PacketRegistry;
import net.openvoxel.server.*;
import net.openvoxel.server.util.CommandInputThread;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

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
	public AtomicBoolean isRunning = new AtomicBoolean(true);
	/**
	 * The Global EventBus
	 */
	private final EventBus eventBus;

	/**
	 * Parsed Reference to the main arguments
	 */
	private static ArgumentParser args;
	/**
	 * The Main Instance of the Game Class
	 */
	private static OpenVoxel instance;

	/**
	 * Currently Running Standard Server
	 */
	@SideOnly(side = Side.DEDICATED_SERVER)
	private Server currentServer = null;

	/**
	 * Currently Running Client Server
	 */
	@SideOnly(side = Side.CLIENT)
	private ClientServer currentClientServer = null;

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
	private UserData userData;

	@PublicAPI
	public static UserData getClientUserData() {
		return instance.userData;
	}

	/**
	 * Main Logger
	 */
	private static Logger openVoxelLogger;


	@PublicAPI
	public static Logger getLogger() {
		return openVoxelLogger;
	}

	/**
	 * Tells the game to bootstrap a server instance
	 * Local Server => Host + Connect
	 * Remote Server => Host
	 * Client Server => Connect
	 * @param server the server instance
	 */
	@PublicAPI
	public void SetCurrentServer(BaseServer server) {
		if(Side.isClient) {
			setCurrentServer_client(server);
		}else{
			setCurrentServer_dedicated(server);
		}
	}

	@SideOnly(side=Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void setCurrentServer_client(BaseServer server) {
		ClientServer newServer = (ClientServer)server;
		if(currentClientServer != null) {
			openVoxelLogger.Severe("Running Server Was Not Shutdown Correctly");
			//currentClientServer.shutdown(); TODO: handle server change
			currentClientServer = null;
		}
	}

	@SideOnly(side=Side.DEDICATED_SERVER,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void setCurrentServer_dedicated(BaseServer server) {
		Server newServer = (Server)server;
		if(currentServer != null) {
			openVoxelLogger.Severe("Running Server Was Not Shutdown Correctly");
			//currentServer.shutdown(); TODO: handle server change
			currentServer = null;
		}
		currentServer = newServer;
	}

	/**
	 *
	 * Connect to a hosted server
	 *
	 * @param address the address of the server
	 */
	public void clientConnectRemote(SocketAddress address) {
		if(currentClientServer != null) {
			currentClientServer.disconnect();
		}
		currentClientServer = new ClientServer();
		//TODO: implement connect
	}

	/**
	 *  Connect to the currently hosted integrated server
	 */
	public void clientConnectLocal() {
		if(currentClientServer != null) {
			currentClientServer.disconnect();
		}
		currentClientServer = new ClientServer();
		//TODO: implement connect
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
	 * @return the current server view from the client
	 */
	@SideOnly(side = Side.CLIENT)
	public static ClientServer getClientServer() {
		return instance.currentClientServer;
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

	public static <T extends AbstractEvent> void unregisterEvent(EventListener listener,Class<T> event) {
		instance.eventBus.unregister(listener,event);
	}

	public static void unregisterAllEvents(EventListener listener) {
		instance.eventBus.unregisterAll(listener);
	}

	/**
	 * @param report a crash
	 */
	public static void reportCrash(CrashReport report) {
		report.printStackTrace();
		FolderUtils.storeCrashReport(report);
		instance.AttemptShutdownSequence(true);
	}

	private AtomicBoolean shutdownIsCrash = new AtomicBoolean(false);


	private AtomicBoolean flagReload = new AtomicBoolean(false);
	/**
	 * Tells the wrapped loader to re-attempt the loading seqyence
	 */
	public static void reloadMods() {
		openVoxelLogger.getSubLogger("Reload").Info("Attempting Mod Reload");
		instance.flagReload.set(true);
		instance.AttemptShutdownSequence(false);
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
		openVoxelLogger = Logger.getLogger("Open Voxel");
		//Quick Config Settup//
		if(args.hasFlag("debugAll")) {
			openVoxelLogger.Info("Enabling All Debug Code");
			args.storeRuntimeFlag("debugChecks");
			args.storeRuntimeFlag("vkDebug");
			args.storeRuntimeFlag("vkDebugDetailed");
			args.storeRuntimeFlag("glDebug");
		}else if(args.hasFlag("perfAll")) {
			args.storeRuntimeFlag("noChecks");
		}
		//Configure Debug Settings//
		if(args.hasFlag("noChecks")) {
			openVoxelLogger.Info("Enabled Minimal Checking Mode");
			System.setProperty("org.lwjgl.glfw.checkThread0","false");
			System.setProperty("org.lwjgl.util.NoChecks","true");
		}else if(args.hasFlag("debugChecks")) {
			openVoxelLogger.Info("Enabled Maximum Debug Mode");
			System.setProperty("org.lwjgl.util.Debug","true");
			args.storeRuntimeFlag("debugAllocator");
			openVoxelLogger.Info("Enabling Debugging Logging");
			args.storeRuntimeFlag("bonusLogging");
		}
		if(args.hasFlag("debugAllocator") && !args.hasFlag("noDebugAllocator")) {
			openVoxelLogger.Info("Enabled LWJGL Debug Memory Allocator");
			System.setProperty("org.lwjgl.util.DebugAllocator","true");
		}
		openVoxelLogger.Info("Loaded Open Voxel "+currentVersion.getValString());
		if(args.hasFlag("bonusLogging")) {
			openVoxelLogger.Info("Enabling Additional Logging Frameworks");
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
		//Run main code//
		if(isClient) {
			clientMain();
		}else{
			serverMain();
		}
		//Fallback for reload//
		if(instance.flagReload.get() && isClient) {
			//Send Runtime Exception Across the Class Loader Barrier
			throw new RuntimeException("built_in_exception::mod_reload");
		}
	}

	@SideOnly(side = Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void clientMain() {
		Renderer renderer = new Renderer();
		ClientAudio.Load();
		GameLoaderThread.StartLoad();
		//TODO: Between
		GameLoaderThread.AwaitLoadFinish();
		//TODO: stitch texture atlas
		blockRegistry.generateMappingsFromRaw();
		if(!args.hasFlag("noBackgroundWorld")) {
			SetCurrentServer(new BackgroundClientServer());
		}
		GUI.addScreen(new ScreenMainMenu());
		try {
			//Run Main Loop//
			while (isRunning.get()) {
				if(currentClientServer != null) {
					//currentClientServer.updateChunks();
				}
			}
		}finally {
			if(currentClientServer != null) {
				//currentClientServer.shutdown();
			}
			renderer.close();
			//ClientAudio.Shutdown();
		}
	}

	@SideOnly(side = Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void serverMain() {
		//CommandInputThread.Start();
		//GameLoaderThread.StartLoad();
		//GameLoaderThread.AwaitLoadFinish();
		//Logger.getLogger("Dedicated Server").Info("Starting Server...");
		//TODO: FIX
		//SetCurrentServer(new DedicatedServer(new GameSave(new File("dedicated_save"))));
		//Logger.getLogger("Dedicated Server").Info("Dedicated NYI... Exiting");
		//while(isRunning.get()) {
			//Main Loop
		//}
	}


	/**
	* Start the Shutdown Sequence, Can be cancelled
	* @param isCrash is the shutdown unexpected, so forcefully exit
	**/
	public void AttemptShutdownSequence(boolean isCrash) {
		shutdownIsCrash.set(isCrash);
		//if(Side.isClient) {
			//try {
				//TODO: ensure correct disconnect cache clearing procedure
				//if (currentClientServer != null) {
				//	currentClientServer.disconnect();
				//}
				//Thread.sleep(100);
		//	}catch(Exception ignored) {
		//		ignored.printStackTrace();
		//	}
		//}
		isRunning.set(false);
	}
	private void AttemptShutdownSequenceInternal(boolean isCrash) {
		WindowCloseRequestedEvent e1 = new WindowCloseRequestedEvent(isCrash);
		eventBus.push(e1);
		if(e1.isCancelled() && isCrash) {
			openVoxelLogger.Warning("Window Close Requested Event for Crash Stopped, Invalid Behaviour");
		}
		if(!e1.isCancelled() || isCrash) {
			//notifyEvent Main Loops Of Shutdown//
			openVoxelLogger.Info("Starting Shutdown Sequence");
			isRunning.set(false);
			if(Side.isClient) {
				//RenderThread.awaitTermination();
				GLFWLogWrapper.Unload();
			}
			try{
				Thread.sleep(3000);//TODO: await more efficiently
			}catch(InterruptedException e) {
				openVoxelLogger.Warning("Thread Wait Was Interrupted");
			}
			//Push Event After Timeout//
			openVoxelLogger.Info("Shutting Down...");
			ProgramShutdownEvent e2 = new ProgramShutdownEvent(isCrash);
			eventBus.push(e2);
			if(isCrash) {
				preCrash();
				System.exit(-1);
			}
		}
	}

	/**
	 * Stop Memory Leak Reports Cluttering up the system information pane on crashes
	 */
	private void preCrash() {
		if(Configuration.DEBUG_MEMORY_ALLOCATOR.get(false)) {
			openVoxelLogger.Info("Hiding Memory Leak Info From Crash Report");
			Object debugAllocator = Reflect.byName("org.lwjgl.system.MemoryUtil$LazyInit").getField("ALLOCATOR").getStatic();
			Object allocationMap = Reflect.on(debugAllocator).get("ALLOCATIONS");
			Reflect.on(allocationMap).invoke("clear");
		}
	}
}
