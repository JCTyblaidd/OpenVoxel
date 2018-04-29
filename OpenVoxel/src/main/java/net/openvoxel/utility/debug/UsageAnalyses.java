package net.openvoxel.utility.debug;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.loader.classloader.Validation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.remotery.RMTSettings;
import org.lwjgl.util.remotery.Remotery;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class UsageAnalyses {

	private static final boolean REMOTERY_ENABLED = OpenVoxel.getLaunchParameters().hasFlag("-enableRemotery");
	private static final Logger logger = Logger.getLogger("Remotery");
	private static long instance = 0;
	private static TObjectIntMap<String> name_offset_map;
	private static IntBuffer hash_storage;

	private static void init_hash_cache() {
		name_offset_map = new TObjectIntHashMap<>();
		hash_storage = MemoryUtil.memAllocInt(1024);
		hash_storage.limit(0);
	}

	private static IntBuffer get_hash_pointer(String name) {
		int val = name_offset_map.get(name);
		if(val == name_offset_map.getNoEntryValue()) {
			val = hash_storage.limit();
			hash_storage.limit(val + 1);
			name_offset_map.put(name, val);
		}
		hash_storage.position(val);
		return hash_storage;
	}

	private static void free_hash_cache() {
		MemoryUtil.memFree(hash_storage);
	}

	@Validation
	public static void Init() {
		if(REMOTERY_ENABLED) {
			init_hash_cache();
			try(MemoryStack stack = stackPush()) {
				PointerBuffer pointer = stack.mallocPointer(1);
				int code = Remotery.rmt_CreateGlobalInstance(pointer);
				if(code != Remotery.RMT_ERROR_NONE) {
					instance = pointer.get(0);
				}else{
					logger.Info("Failed to Initialize: #"+Integer.toHexString(code));
					instance = 0;
				}
			}
			if(instance != 0) {
				logger.Info("Successfully Initialized");
				Remotery.rmt_SetGlobalInstance(instance);
				RMTSettings settings = Remotery.rmt_Settings();
				if(settings != null) {
					settings.port((short) 100);
					settings.limit_connections_to_localhost(1);
					settings.msSleepBetweenServerUpdates(20);
					settings.messageQueueSizeInBytes(128 * 1024);
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
			}
		}
	}

	@Validation
	public static void Shutdown() {
		if(REMOTERY_ENABLED && instance != 0) {
			Remotery.rmt_DestroyGlobalInstance(instance);
			Remotery.rmt_SetGlobalInstance(0L);
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
