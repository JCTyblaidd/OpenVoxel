package net.openvoxel.files.util;

import com.jc.util.filesystem.FileHandle;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.files.world.GameSave;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.lwjgl.stb.STBImageWrite.stbi_write_png;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png_to_func;

/**
 * Created by James on 10/09/2016.
 *
 * Folder Information Cache
 */
public class FolderUtils {

	public static final File ConfigDir;
	public static final File SaveDir;
	public static final File ResourceDir;
	public static final File ScreenshotsDir;
	public static final File StatsDir;
	public static final File LogsDir;
	public static final File CrashDir;
	private static final Logger folderLogger = Logger.getLogger("Folder Manager");
	private static final Logger saveManagerLogger = Logger.getLogger("Save Manager");
	static {
		ConfigDir = getFolder("config");
		SaveDir = getFolder("save");
		ResourceDir = getFolder("resource_packs");
		ScreenshotsDir = getFolder("screenshots");
		StatsDir = getFolder("stats");
		LogsDir = getFolder("logs");
		CrashDir = getFolder("crash_reports");
	}
	private static File getFolder(String str) {
		File f = new File(str);
		if(f.mkdir()) {
			folderLogger.Info("Created Folder: " + str);
		}
		return f;
	}

	private static void storeImageInFile(File f,int w, int h, ByteBuffer data) {
		try {
			FileOutputStream fileOut = new FileOutputStream(f);
			FileChannel channel = fileOut.getChannel();
			STBIWriteCallback _callback_ = new STBIWriteCallback() {
				@Override
				public void invoke(long context, long data, int size) {
					ByteBuffer buffer = getData(data,size);
					try {
						channel.write(buffer);
					}catch(Exception ex) {
						folderLogger.Warning("Exception caught while writing to file");
					}
				}
			};
			stbi_write_png_to_func(_callback_,0L,h,w,4,data,0);
			_callback_.close();
			channel.close();
		}catch (Exception ex) {
			folderLogger.Warning("Failed to store image to file");
		}
	}

	/*
	 * Save a stitched texture result
	 */
	public static void saveTextureStitch(int w, int h,ByteBuffer data,String name,int mip_levels) {
		if(mip_levels > 1) {
			if(w != h) {
				folderLogger.Warning("Attempted to call mip map texture stitch on target where w != h");
				return;
			}
			int size = w;
			int offset =0 ;
			int NUM = 0;
			while(NUM < mip_levels && size > 0) {
				File f = new File(ResourceDir,name+"-"+NUM+".png");
				data.position(4 * offset);
				storeImageInFile(f,size,size,data);
				//NEXT
				offset += size * size;
				NUM += 1;
				size /= 2;
			}
			data.position(0);
		}else {
			File f = new File(ResourceDir,name+".png");
			storeImageInFile(f,w,h,data);
		}
	}

	public static void saveScreenshot(int w, int h,int[] pixels,boolean swizzle_result) {
		ByteBuffer byteData = MemoryUtil.memAlloc(w*h*4);
		try {
			byteData.asIntBuffer().put(pixels);
			byteData.position(0);
			saveScreenshot(w, h, byteData, swizzle_result);
		}finally {
			MemoryUtil.memFree(byteData);
		}
	}

	public static void saveScreenshot(GraphicsAPI.ScreenshotInfo screenshotInfo) {
		//TODO: SWIZZLE OR NOT TO SWIZZLE???
		saveScreenshot(screenshotInfo.width,screenshotInfo.height,screenshotInfo.bytes,true);
	}

	public static void saveScreenshot(int w, int h,ByteBuffer pixels,boolean swizzle_result) {
		ByteBuffer copy_buffer = pixels;
		if(swizzle_result) {
			copy_buffer = MemoryUtil.memAlloc(w * h * 4);
		}
		try {
			Date d = new Date();
			Random rand = new Random();
			DateFormat format = DateFormat.getDateTimeInstance();
			File f = new File(ScreenshotsDir, "screenshot " + format.format(d).replace(':', '-') + "-" + rand.nextInt(99) + ".png");

			if(swizzle_result) {
				for(int i = 0; i < w * h; i++) {
					final int loc = i * 4;
					copy_buffer.put(loc,pixels.get(loc+2));
					copy_buffer.put(loc+1,pixels.get(loc+1));
					copy_buffer.put(loc+2,pixels.get(loc));
					copy_buffer.put(loc+3,pixels.get(loc+3));
				}
				copy_buffer.position(0);
			}

			stbi_write_png(f.getAbsolutePath(), w, h, 4, copy_buffer, 0);
		}finally {
			if(swizzle_result) {
				MemoryUtil.memFree(copy_buffer);
			}
		}
	}

	public static List<String> listAllGameSaves() {
		File[] subFiles = SaveDir.listFiles();
		List<String> saves = new ArrayList<>();
		if(subFiles == null) return saves;
		for(File f : subFiles) {
			saves.add(f.getName());
		}
		return saves;
	}

	public static GameSave loadGameSave(String saveName) {
		File f = new File(SaveDir,saveName);
		if(!f.exists()) {
			saveManagerLogger.Severe("Loading Imaginary Save File");
			throw new RuntimeException("bad save create");
		}
		return new GameSave(f);
	}

	public static GameSave newSave(String saveName) {
		File f = new File(SaveDir,saveName);
		if(f.exists()) {
			saveManagerLogger.Severe("Save exists where new one was requested");
			saveManagerLogger.Info("Result - the save was loaded instead");
		}
		return new GameSave(f);
	}

	public static void storeCrashReport(CrashReport report) {
		File crash = new File(CrashDir,"crash-"+new Date().getTime()+".txt");
		FileHandle handle = new FileHandle(crash);
		handle.startWrite();
		handle.write(report.toString());
		handle.stopWrite();
	}
}
