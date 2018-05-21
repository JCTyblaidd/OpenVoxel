package net.openvoxel.utility.debug;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.loader.classloader.Validation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.remotery.RMTSettings;
import org.lwjgl.util.remotery.Remotery;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class UsageAnalyses {

	private static final boolean REMOTERY_ENABLED;
	private static final Logger logger = Logger.getLogger("Remotery");
	private static long instance = 0;

	static {
		//Required to allow for testing
		ArgumentParser parser = OpenVoxel.getLaunchParameters();
		if(parser != null) REMOTERY_ENABLED = parser.hasFlag("enableRemotery");
		else REMOTERY_ENABLED = false;
	}

	@Validation
	private static void init_hash_cache() {
		//TODO: IMPLEMENT IN THREAD SAFE METHOD
	}

	@Validation
	private static IntBuffer get_hash_pointer(String name) {
		return null;
	}

	@Validation
	private static void free_hash_cache() {
		//TODO: IMPLEMENT IN THREAD SAFE METHOD
	}

	@Validation
	public static void Init() {
		if(REMOTERY_ENABLED) {
			init_hash_cache();
			RMTSettings settings = Remotery.rmt_Settings();
			if(settings != null) {
				settings.port((short) 17815);
				settings.limit_connections_to_localhost(1);
				settings.msSleepBetweenServerUpdates(20);
				settings.messageQueueSizeInBytes(512 * 1024);
				settings.maxNbMessagesPerUpdate(10);
				/*
				settings._malloc();
				settings.realloc();
				settings._free();
				settings.mm_context();
				settings.input_handler();
				settings.input_handler_context();
				settings.logFilename("rmtLog.txt");
				*/
			}
			try(MemoryStack stack = stackPush()) {
				PointerBuffer pointer = stack.mallocPointer(1);
				int code = Remotery.rmt_CreateGlobalInstance(pointer);
				if(code == Remotery.RMT_ERROR_NONE) {
					instance = pointer.get(0);
				}else{
					logger.Info("Failed to Initialize: #"+Integer.toHexString(code));
					instance = 0;
				}
			}
			if(instance != 0) {
				logger.Info("Successfully Initialized");
				Remotery.rmt_SetGlobalInstance(instance);
			}
		}
	}

	@Validation
	public static void Shutdown() {
		if(REMOTERY_ENABLED && instance != 0) {
			Remotery.rmt_DestroyGlobalInstance(instance);
			logger.Info("Successfully Shutdown");
			free_hash_cache();
		}
	}

	@Validation
	public static void SetThreadName(String name) {
		if(REMOTERY_ENABLED && instance != 0) {
			Remotery.rmt_SetCurrentThreadName(name);
		}
	}

	@Validation
	public static void StartCPUSample(String name,int flags) {
		if(REMOTERY_ENABLED && instance != 0) {
			Remotery.rmt_BeginCPUSample(name,flags,get_hash_pointer(name));
		}
	}

	@Validation
	public static void StopCPUSample() {
		if(REMOTERY_ENABLED && instance != 0) {
			Remotery.rmt_EndCPUSample();
		}
	}

}
