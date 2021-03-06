package net.openvoxel.launchwrapper;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.loader.classloader.SideSpecificTweaker;
import net.openvoxel.loader.classloader.TweakableClassLoader;
import net.openvoxel.loader.mods.ModDataLoader;
import net.openvoxel.loader.optimizer.Optimizer;
import net.openvoxel.utility.debug.RenderDocAutoHook;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Created by James on 25/08/2016.
 *
 * Common Launching Functionality
 */
class CommonLauncher {

	/**
	 * Await Garbage Collection
	 */
	private static void awaitClassGarbageCollection(WeakReference<TweakableClassLoader> gcTarget) {
		System.gc();
		while(!gcTarget.isEnqueued()) {
			try{
				Thread.sleep(100);
			}catch (Exception ignored) {}
		}
	}

	static void defaultLaunch(String[] args, boolean isClient) {
		if(new ArgumentParser(args).hasFlag("renderDocWait")) {
			RenderDocAutoHook.callRenderDocInject();
		}
		boolean reloadRequest;
		do {
			Logger loaderLogger = Logger.getLogger("Initialisation");
			CommonLauncher.EnableClassLoader(isClient);
			ModDataLoader modData = CommonLauncher.EnableMods(new File("mods"));
			loaderLogger.Info("Found " + modData.getLoadedModCount() + " mods");
			String[] asmList = modData.getASMModifierList();
			String[] classList = modData.getClassList();
			modData.handleASMDependencies(asmList);
			reloadRequest = CommonLauncher.StartOpenVoxelClassLoaded(args, classList, asmList, isClient);
			//Cleanup//
			//TweakableClassLoader.INSTANCE.unregisterAllTransformers();
			//TweakableClassLoader.INSTANCE.unloadLibraries();
			//WeakReference<TweakableClassLoader> weakReference = new WeakReference<>(TweakableClassLoader.INSTANCE);
			//TweakableClassLoader.INSTANCE = null;
			//awaitClassGarbageCollection(weakReference);
		}while (reloadRequest);
		System.exit(0);
	}


	private static void EnableClassLoader(boolean isClient) {
		TweakableClassLoader.Load();
		TweakableClassLoader.INSTANCE.registerTransformer(new SideSpecificTweaker(isClient));
	}

	private static ModDataLoader EnableMods(File mod_folder) {
		ModDataLoader modLoader = new ModDataLoader();
		modLoader.scanDirectoryForMods(mod_folder);
		modLoader.scanLoadedMods();
		return modLoader;
	}

	private static final String OpenVoxelClass = "net.openvoxel.OpenVoxel";
	private static final String VanillaClass = "net.openvoxel.vanilla.Vanilla";
	private static final String ReloadExceptionKey = "built_in_exception::mod_reload";

	private static boolean StartOpenVoxelClassLoaded(String[] args, String[] mod_args, String[] asm_args, boolean isClient) {
		try {
			ArgumentParser argParser = new ArgumentParser(args);
			if(!argParser.hasFlag("noVanillaMod")) {
				mod_args = Arrays.copyOf(mod_args,mod_args.length+1);
				mod_args[mod_args.length-1]=VanillaClass;
				Logger.INSTANCE.Info("Added Vanilla");
			}
			if(argParser.hasFlag("optimizeBytecode")) {
				TweakableClassLoader.INSTANCE.registerTransformer(new Optimizer(argParser));
			}
			if(argParser.hasFlag("noASM")) {//Allow Option of no ASM: for debug purposes
				TweakableClassLoader.INSTANCE.unregisterAllTransformers();
			}
			Class<?> clz = TweakableClassLoader.INSTANCE.loadClass(OpenVoxelClass);
			try {
				clz.getConstructor(String[].class, String[].class, String[].class, boolean.class).newInstance(args, mod_args, asm_args, isClient);
			}catch (InvocationTargetException ex) {
				if(ex.getCause().getClass() == RuntimeException.class) {
					if(ex.getCause().getMessage().equals(ReloadExceptionKey)) {
						Logger.getLogger("Loader").Info("Attempting Reload");
						return true;
					}else{
						throw ex;
					}
				}else{
					throw ex;
				}
			}
		}catch(Exception e) {
			if(e instanceof ClassNotFoundException) {
				ClassNotFoundException noClass = (ClassNotFoundException) e;
				System.out.println(noClass.getMessage());
				if(OpenVoxelClass.equals(noClass.getMessage())) {
					Logger.INSTANCE.Severe("Launch Error: Failed To ClassLoad Main Entry Point");
					System.exit(-1);
				}
			}
			Logger.INSTANCE.Severe("Uncaught Exception!");
			Logger.INSTANCE.StackTrace(e);
			System.exit(-1);
		}
		return false;
	}

}
