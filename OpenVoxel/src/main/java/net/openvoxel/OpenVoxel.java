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
import net.openvoxel.common.GameLoader;
import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.common.event.EventBus;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.window.ProgramShutdownEvent;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.registry.RegistryEntities;
import net.openvoxel.common.registry.RegistryItems;
import net.openvoxel.files.FolderUtils;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.networking.protocol.PacketRegistry;
import net.openvoxel.server.BackgroundClientServer;
import net.openvoxel.server.BaseServer;
import net.openvoxel.server.ClientServer;
import net.openvoxel.server.DedicatedServer;
import net.openvoxel.server.util.CommandInputThread;
import net.openvoxel.utility.AsyncBarrier;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.debug.UsageAnalyses;
import org.lwjgl.system.Configuration;

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
	 * Is the game currently Running (in main loop)
	 */
	private  AtomicBoolean isRunning = new AtomicBoolean(true);

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
	private DedicatedServer currentServer = null;

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
			openVoxelLogger.Info("Changing current Server: Client");
		}
	}

	@SideOnly(side=Side.DEDICATED_SERVER,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void setCurrentServer_dedicated(BaseServer server) {
		DedicatedServer newServer = (DedicatedServer)server;
		if(currentServer != null) {
			openVoxelLogger.Info("Changing current Server: Dedicated");
		}
		currentServer = newServer;
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
	public static DedicatedServer getServer() {
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
	 * @param report a crash: throws an exception to unwind the stack
	 */
	public static void reportCrash(CrashReport report) throws RuntimeException{
		FolderUtils.storeCrashReport(report);
		instance.AttemptShutdownSequence(true);
		throw report.getThrowable();
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

		//Start Usage Analysis
		UsageAnalyses.Init();
		UsageAnalyses.SetThreadName("Main");

		//Run main code//
		if(isClient) {
			UsageAnalyses.StartCPUSample("clientMain()",0);
			clientMain();
			UsageAnalyses.StopCPUSample();
		}else{
			UsageAnalyses.StartCPUSample("serverMain()",0);
			serverMain();
			UsageAnalyses.StopCPUSample();
		}

		//Stop Usage Analysis
		UsageAnalyses.Shutdown();

		//Finish Shutdown
		AttemptShutdownSequenceInternal(shutdownIsCrash.get());

		//Reload functionality support
		if(instance.flagReload.get() && isClient) {
			//Send Runtime Exception Across the Class Loader Barrier
			throw new RuntimeException("built_in_exception::mod_reload");
		}
	}

	@SideOnly(side = Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void clientMain() {
		//Init generic handling code
		Renderer renderer = new Renderer();
		ClientAudio.Load();
		GameLoader.LoadGameStateClient(renderer);

		//Run initial generic setup code
		renderer.stitchAtlas();
		blockRegistry.generateMappingsFromRaw();

		//Set Default Conditions//
		if(!args.hasFlag("noBackgroundWorld")) {
			SetCurrentServer(new BackgroundClientServer());
		}
		GUI.addScreen(new ScreenMainMenu());

		//Update Tick State//
		ClientServer lastServer = null;
		AsyncBarrier updateServerBarrier = new AsyncBarrier();
		AsyncBarrier drawnChunksBarrier = new AsyncBarrier();
		AsyncBarrier drawnGuiBarrier = new AsyncBarrier();
		AsyncBarrier drawCompletionBarrier = new AsyncBarrier();
		try {
			//Run Main Loop//
			UsageAnalyses.StartCPUSample("while(isRunning)",0);
			while (isRunning.get()) {
				//Handle server changes
				if(lastServer != currentClientServer) {
					if(lastServer != null) {
						renderer.invalidateAllChunks();
						lastServer.shutdown();
					}
					if(currentClientServer != null) {
						currentClientServer.startup();
					}
					lastServer = currentClientServer;
				}
				//Main Loop//
				if(lastServer != null) {
					lastServer.serverTick(updateServerBarrier);
				}
				renderer.pollInputs();
				updateServerBarrier.awaitCompletion();
				renderer.prepareFrame();
				renderer.generateUpdatedChunks(lastServer,drawnChunksBarrier);
				if(lastServer != null) {
					lastServer.sendUpdates();
				}
				renderer.startAsyncGUIDraw(drawnGuiBarrier);
				drawnChunksBarrier.awaitCompletion();
				drawnGuiBarrier.awaitCompletion();
				renderer.submitFrame(drawCompletionBarrier);
			}
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Error in Main Loop");
			report.caughtException(ex);
			report.getThrowable().printStackTrace();
			shutdownIsCrash.set(true);
		}finally {
			UsageAnalyses.StopCPUSample();
			if(lastServer != currentClientServer && lastServer != null) {
				lastServer.shutdown();
			}
			if(currentClientServer != null) {
				currentClientServer.shutdown();
			}
			renderer.invalidateAllChunks();
			renderer.close();
			ClientAudio.Unload();
		}
	}

	@SideOnly(side = Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void serverMain() {
		CommandInputThread.Start();
		GameLoader.LoadGameStateServer();
		Logger dedicatedLogger = Logger.getLogger("Server");
		dedicatedLogger.Info("Starting Server...");

		//Set Default State//
		SetCurrentServer(null);//TODO: IMPLEMENT DEDICATED SERVER
		DedicatedServer lastServer = null;
		AsyncBarrier updateServerBarrier = new AsyncBarrier();
		try{
			//Main Loop
			UsageAnalyses.StartCPUSample("while(isRunning)",0);
			while(isRunning.get()) {
				//Handle Server Changes
				if (lastServer != currentServer) {
					if (lastServer != null) {
						lastServer.shutdown();
					}
					if (currentServer != null) {
						currentServer.startup();
					}
					lastServer = currentServer;
				}
				//Update Server
				if (lastServer != null) {
					lastServer.serverTick(updateServerBarrier);
					updateServerBarrier.awaitCompletion();
					lastServer.sendUpdates();
				}else{
					//Prevent spinning for non-server
					Thread.sleep(100);
				}
			}
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Error in main loop");
			report.caughtException(ex);
			shutdownIsCrash.set(true);
		}finally {
			UsageAnalyses.StopCPUSample();
			if(lastServer != currentServer && lastServer != null) {
				lastServer.shutdown();
			}
			if(currentServer != null) {
				currentServer.shutdown();
			}
			CommandInputThread.Stop();
		}
	}


	/**
	* Start the Shutdown Sequence, Can be cancelled
	* @param isCrash is the shutdown unexpected, so forcefully exit
	**/
	public void AttemptShutdownSequence(boolean isCrash) {
		shutdownIsCrash.set(isCrash);
		openVoxelLogger.Info("Starting Shutdown Sequence");
		isRunning.set(false);
	}

	private void AttemptShutdownSequenceInternal(boolean isCrash) {
		//notifyEvent Main Loops Of Shutdown//
		openVoxelLogger.Info("Finishing Shutdown Sequence");
		if(Side.isClient) {
			GLFWLogWrapper.Unload();
		}
		//Push Event After Timeout//
		openVoxelLogger.Info("Calling Shutdown Event");
		ProgramShutdownEvent e2 = new ProgramShutdownEvent(isCrash);
		eventBus.push(e2);
		openVoxelLogger.Info("Finished Shutdown Event");
		if(isCrash) {
			try {
				openVoxelLogger.Info("Preventing Memory Dump on Crash");
				preCrash();
			}catch(Exception ex) {
				openVoxelLogger.Warning("Failed to Prevent Memory Dump on Crash");
				ex.printStackTrace();
			}
			openVoxelLogger.Info("Forcefully Exiting");
			System.exit(-1);
		}
		openVoxelLogger.Info("Shutting Down");
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
