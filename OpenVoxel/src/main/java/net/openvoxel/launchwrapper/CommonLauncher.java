package net.openvoxel.launchwrapper;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.loader.classloader.SideSpecificTweaker;
import net.openvoxel.loader.classloader.TweakableClassLoader;
import net.openvoxel.loader.mods.ModDataLoader;

import java.io.File;
import java.util.Arrays;

/**
 * Created by James on 25/08/2016.
 *
 * Common Launching Functionality
 */
public class CommonLauncher {

	static void defaultLaunch(String[] args, boolean isClient) {
		Logger loaderLogger = Logger.getLogger("Initialisation");
		CommonLauncher.EnableClassLoader(isClient);
		ModDataLoader modData = CommonLauncher.EnableMods(new File("mods"));
		loaderLogger.Info("Found " + modData.getLoadedModCount() + " mods");
		String[] asmList = modData.getASMModifierList();
		String[] classList = modData.getClassList();
		modData.handleASMDependencies(asmList);
		CommonLauncher.StartOpenVoxelClassLoaded(args,classList,asmList,isClient);
	}


	static void EnableClassLoader(boolean isClient) {
		TweakableClassLoader.Load();
		TweakableClassLoader.INSTANCE.registerTransformer(new SideSpecificTweaker(isClient));
	}

	static ModDataLoader EnableMods(File mod_folder) {
		ModDataLoader modLoader = new ModDataLoader();
		modLoader.scanDirectoryForMods(mod_folder);
		modLoader.scanLoadedMods();
		return modLoader;
	}

	static final String OpenVoxelClass = "net.openvoxel.OpenVoxel";
	static final String VanillaClass = "net.openvoxel.vanilla.Vanilla";

	static void StartOpenVoxelClassLoaded(String[] args, String[] mod_args,String[] asm_args, boolean isClient) {
		try {
			ArgumentParser argParser = new ArgumentParser(args);
			if(!argParser.hasFlag("noVanillaMod")) {
				mod_args = Arrays.copyOf(mod_args,mod_args.length+1);
				mod_args[mod_args.length-1]=VanillaClass;
				Logger.INSTANCE.Info("Added Vanilla");
			}
			if(argParser.hasFlag("noASM")) {//Allow Option of no ASM: for debug purposes
				TweakableClassLoader.INSTANCE.unregisterAllTransformers();
			}
			Class<?> clz = TweakableClassLoader.INSTANCE.loadClass(OpenVoxelClass);
			clz.getConstructor(String[].class,String[].class,String[].class,boolean.class).newInstance(args,mod_args,asm_args,isClient);
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
	}

}
