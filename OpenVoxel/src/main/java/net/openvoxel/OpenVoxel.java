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
import net.openvoxel.client.gui.framework.GUI;
import net.openvoxel.client.gui.menu.ScreenMainMenu;
import net.openvoxel.client.renderer.Renderer;
import net.openvoxel.common.GameLoader;
import net.openvoxel.common.event.AbstractEvent;
import net.openvoxel.common.event.EventBus;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.window.ProgramShutdownEvent;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.registry.RegistryEntities;
import net.openvoxel.common.registry.RegistryItems;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.files.util.FolderUtils;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.networking.protocol.PacketRegistry;
import net.openvoxel.server.BackgroundClientServer;
import net.openvoxel.server.BaseServer;
import net.openvoxel.server.ClientServer;
import net.openvoxel.server.DedicatedServer;
import net.openvoxel.server.util.CommandInputThread;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.debug.UsageAnalyses;
import net.openvoxel.utility.debug.Validate;
import org.jetbrains.annotations.Nullable;
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
	 * Currently Running Server {Client is Side.isClient, Standard otherwise}
	 */
	private BaseServer currentServer;

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
	public void SetCurrentServer(@Nullable BaseServer server) {
		Validate.IsMainThread();
		if(Side.isClient) {
			Validate.Condition(server == null || server instanceof ClientServer,"Must be ClientServer if Side.isClient");
		}else{
			Validate.Condition(server == null || server instanceof DedicatedServer,"Must be DedicatedServer id !Side.isClient");
		}
		currentServer = server;
	}

	/**
	 * @return the current instance
	 */
	@PublicAPI
	public static OpenVoxel getInstance() {
		return instance;
	}

	/**
	 * @return the argument parser
	 */
	@PublicAPI
	public static ArgumentParser getLaunchParameters() {
		return args;
	}

	/*
	 * Side agnostic version of getServer()
	 */
	@PublicAPI
	public static BaseServer getServer() {
		return instance.currentServer;
	}

	/**
	 * @return the current server, NULL if it doesn't exist
	 */
	@PublicAPI
	@SideOnly(side = Side.DEDICATED_SERVER)
	public static DedicatedServer getDedicatedServer() {
		return (DedicatedServer)getServer();
	}

	/**
	 * @return the current server view from the client
	 */
	@PublicAPI
	@SideOnly(side = Side.CLIENT)
	public static ClientServer getClientServer() {
		return (ClientServer)getServer();
	}

	/**
	 * @param event call an event through the eventBus
	 */
	@PublicAPI
	public static void pushEvent(AbstractEvent event) {
		Validate.IsMainThread();
		instance.eventBus.push(event);
	}

	/**
	 * @param listener The Event Listener to register for events
	 */
	@PublicAPI
	public static void registerEvents(EventListener listener) {
		Validate.IsMainThread();
		instance.eventBus.register(listener);
	}

	@PublicAPI
	public static <T extends AbstractEvent> void unregisterEvent(EventListener listener,Class<T> event) {
		Validate.IsMainThread();
		instance.eventBus.unregister(listener,event);
	}

	@PublicAPI
	public static void unregisterAllEvents(EventListener listener) {
		Validate.IsMainThread();
		instance.eventBus.unregisterAll(listener);
	}

	/**
	 * @param report a crash: throws an exception to unwind the stack
	 */
	@PublicAPI
	public static void reportCrash(CrashReport report) throws RuntimeException{
		FolderUtils.storeCrashReport(report);
		instance.AttemptShutdownSequence(true);
		throw report.getThrowable();
	}

	private AtomicBoolean shutdownIsCrash = new AtomicBoolean(false);


	private AtomicBoolean flagReload = new AtomicBoolean(false);
	/**
	 * Tells the wrapped loader to re-attempt the loading sequence
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

		//Setup-Validation
		Validate.SetAsMainThread();

		//Quick Config Set-up//
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
			clientMain();
		}else{
			serverMain();
		}

		//Stop Usage Analysis
		UsageAnalyses.Shutdown();

		//Free up Resources
		ResourceManager.unloadAll();

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
		final Renderer renderer;
		try {
			renderer = new Renderer();
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Failed to Load Renderer");
			report.caughtException(ex);
			report.getThrowable().printStackTrace();
			return;
		}

		try {
			ClientAudio.Load();
			GameLoader.LoadGameStateClient(renderer);
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Failed to Init Client Game State");
			report.caughtException(ex);
			report.getThrowable().printStackTrace();
			renderer.close();
			return;
		}

		//Run initial generic setup code
		renderer.stitchAtlas();
		blockRegistry.generateMappingsFromRaw();

		//Set Default Conditions//
		if(args.hasFlag("noBackgroundWorld")) {
			SetCurrentServer(null);
		}else{
			SetCurrentServer(new BackgroundClientServer());
		}
		GUI.addScreen(new ScreenMainMenu(renderer));

		//Update Tick State//
		ClientServer lastServer = null;
		AsyncBarrier updateServerBarrier = new AsyncBarrier();
		AsyncBarrier drawnChunksBarrier = new AsyncBarrier();
		AsyncBarrier drawnGuiBarrier = new AsyncBarrier();
		AsyncBarrier drawCompletionBarrier = new AsyncBarrier();
		try {
			//Run Main Loop//
			while (isRunning.get()) {
				UsageAnalyses.StartCPUSample("Main Loop",0);
				//Handle server changes
				if(lastServer != currentServer) {
					if(lastServer != null) {
						{
							UsageAnalyses.StartCPUSample("Render Invalidate", 0);
							renderer.invalidateAllChunks();
							UsageAnalyses.StopCPUSample();
						}
						{
							UsageAnalyses.StartCPUSample("Server Stop", 0);
							lastServer.shutdown();
							UsageAnalyses.StopCPUSample();
						}
					}
					if(currentServer != null) {
						UsageAnalyses.StartCPUSample("Server Start",0);
						currentServer.startup();
						UsageAnalyses.StopCPUSample();
					}
					lastServer = (ClientServer)currentServer;
				}
				//Main Loop//
				if(lastServer != null) {
					UsageAnalyses.StartCPUSample("Server Tick",0);
					lastServer.serverTick(updateServerBarrier);
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Input Poll",0);
					renderer.pollInputs();
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Server Await",0);
					updateServerBarrier.awaitCompletion();
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Prepare Frame",0);
					renderer.prepareFrame();
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Render World",0);
					renderer.generateUpdatedChunks(lastServer, drawnChunksBarrier);
					UsageAnalyses.StopCPUSample();
				}
				if(lastServer != null) {
					UsageAnalyses.StartCPUSample("Server IO",0);
					lastServer.sendUpdates();
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Render GUI",0);
					renderer.startAsyncGUIDraw(drawnGuiBarrier);
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Await World",0);
					drawnChunksBarrier.awaitCompletion();
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Await GUI",0);
					drawnGuiBarrier.awaitCompletion();
					UsageAnalyses.StopCPUSample();
				}
				{
					UsageAnalyses.StartCPUSample("Submit Frame",0);
					renderer.submitFrame(drawCompletionBarrier);
					UsageAnalyses.StopCPUSample();
				}
				UsageAnalyses.StopCPUSample();
			}
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Error in Main Loop");
			report.caughtException(ex);
			report.getThrowable().printStackTrace();
			shutdownIsCrash.set(true);
		}finally {
			if(lastServer != currentServer && lastServer != null) {
				lastServer.shutdown();
			}
			if(currentServer != null) {
				currentServer.shutdown();
			}
			renderer.invalidateAllChunks();
			renderer.close();
			ClientAudio.Unload();
		}
	}

	@SideOnly(side = Side.CLIENT,operation = SideOnly.SideOperation.REMOVE_CODE)
	private void serverMain() {
		CommandInputThread.Start();
		try {
			GameLoader.LoadGameStateServer();
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Failed to Init Server Game State");
			report.caughtException(ex);
			report.getThrowable().printStackTrace();
			return;
		}

		Logger dedicatedLogger = Logger.getLogger("Server");
		dedicatedLogger.Info("Starting Server...");

		//Set Default State//
		SetCurrentServer(null);//TODO: IMPLEMENT DEDICATED SERVER
		DedicatedServer lastServer = null;
		AsyncBarrier updateServerBarrier = new AsyncBarrier();
		try{
			//Main Loop
			while(isRunning.get()) {
				UsageAnalyses.StartCPUSample("Main Loop",0);
				//Handle Server Changes
				if (lastServer != currentServer) {
					if (lastServer != null) {
						UsageAnalyses.StartCPUSample("Server Stop",0);
						lastServer.shutdown();
						UsageAnalyses.StopCPUSample();
					}
					if (currentServer != null) {
						UsageAnalyses.StartCPUSample("Server Start",0);
						currentServer.startup();
						UsageAnalyses.StopCPUSample();
					}
					lastServer = (DedicatedServer)currentServer;
				}
				//Update Server
				if (lastServer != null) {
					{
						UsageAnalyses.StartCPUSample("Server Tick",0);
						lastServer.serverTick(updateServerBarrier);
						UsageAnalyses.StopCPUSample();
					}
					{
						UsageAnalyses.StartCPUSample("Server Await",0);
						updateServerBarrier.awaitCompletion();
						UsageAnalyses.StopCPUSample();
					}
					{
						UsageAnalyses.StartCPUSample("Server IO",0);
						lastServer.sendUpdates();
						UsageAnalyses.StopCPUSample();
					}
				}else{
					//Prevent spinning for non-server
					Thread.sleep(100);
				}
				UsageAnalyses.StopCPUSample();
			}
		}catch(Exception ex) {
			CrashReport report = new CrashReport("Error in main loop");
			report.caughtException(ex);
			shutdownIsCrash.set(true);
		}finally {
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
		if (Configuration.DEBUG_MEMORY_ALLOCATOR.get(false)) {
			openVoxelLogger.Info("Hiding Memory Leak Info From Crash Report");
			Object debugAllocator = Reflect.byName("org.lwjgl.system.MemoryUtil$LazyInit").getField("ALLOCATOR").getStatic();
			Object allocationMap = Reflect.on(debugAllocator).get("ALLOCATIONS");
			Reflect.on(allocationMap).invoke("clear");
		}
	}
}
