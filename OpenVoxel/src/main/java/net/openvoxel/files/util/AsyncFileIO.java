package net.openvoxel.files.util;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.utility.async.AsyncRunnablePool;
import net.openvoxel.utility.async.AsyncTaskPool;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Created by James on 10/04/2017.
 *
 * Asynchronous File Modification Utilities
 */
public class AsyncFileIO {

	private static final AsyncTaskPool fileIOPool;
	private static final Logger ioLogger;
	static {
		fileIOPool = new AsyncRunnablePool("File-IO",AsyncRunnablePool.getWorkerCount("IOThreadCount",2));
		ioLogger = Logger.getLogger("File IO");
	}

	/**
	 * Asynchronous Store
	 * @param file the target file to write to
	 * @param buffer the buffer to write using (NOT CLEARED)
	 */
	@PublicAPI
	public static void asyncStore(File file, ByteBuffer buffer) {
		addTask(() -> {
			try (FileOutputStream file_out = new FileOutputStream(file)) {
				file_out.getChannel().write(buffer);
			}catch (Exception ex) {
				ioLogger.Severe("Failed To Execute File IO");
				ioLogger.StackTrace(ex);
			}
		});
	}

	/**
	 * @param job to add to the async file io thread pool
	 */
	@PublicAPI
	public static void addTask(Runnable job) {
		fileIOPool.addWork(job);
	}

	/**
	 * Asynchronous Store & Free the Memory
	 * @param file the target file to write to
	 * @param buffer the buffer to write and clear
	 */
	@PublicAPI
	public static void asyncStoreFree(File file,ByteBuffer buffer) {
		addTask(() -> {
			try (FileOutputStream file_out = new FileOutputStream(file)) {
				file_out.getChannel().write(buffer);
				MemoryUtil.memFree(buffer);
			}catch (Exception ex) {
				ioLogger.Severe("Failed To Execute File IO");
				ioLogger.StackTrace(ex);
			}
		});
	}

	/**
	 * Asynchronously read the file's contents
	 * @param file the target file to read
	 * @param data function to use the data
	 */
	@PublicAPI
	public static void asyncRead(File file, Consumer<ByteBuffer> data) {
		addTask(() -> {
			try(FileInputStream fin = new FileInputStream(file)) {
				int size = (int)fin.getChannel().size();
				ByteBuffer buf = MemoryUtil.memAlloc(size);
				fin.getChannel().read(buf);
				data.accept(buf);
				MemoryUtil.memFree(buf);
			}catch(Exception ex) {
				ioLogger.Severe("Failed to Execute File IO");
				ioLogger.StackTrace(ex);
			}
		});
	}

}
