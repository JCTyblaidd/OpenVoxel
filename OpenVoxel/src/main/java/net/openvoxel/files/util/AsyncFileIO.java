package net.openvoxel.files.util;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.utility.AsyncRunnablePool;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by James on 10/04/2017.
 *
 * Asynchronous File Modification Utilities
 */
public class AsyncFileIO {

	private static final AsyncRunnablePool fileIOPool;
	private static final Logger ioLogger;
	static {
		fileIOPool = new AsyncRunnablePool("File-IO",2);
		ioLogger = Logger.getLogger("File IO");
	}
	public static void asyncStore(File file, ByteBuffer buffer) {
		addTask(() -> {
			try {
				FileOutputStream fout = new FileOutputStream(file);
				fout.getChannel().write(buffer);
				fout.close();
			}catch (Exception ex) {
				ioLogger.Severe("Failed To Execute File IO");
				ioLogger.StackTrace(ex);
			}
		});
	}

	public static void addTask(Runnable job) {
		fileIOPool.addWork(job);
	}

	/**
	 * Asynchronous Store & Free the Memory
	 */
	public static void asyncStoreFree(File file,ByteBuffer buffer) {
		asyncStore(file, buffer);
		MemoryUtil.memFree(buffer);
	}

}
