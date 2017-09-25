package net.openvoxel.files;

import com.jc.util.filesystem.FileHandle;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

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

	public static void saveScreenshot(int w, int h,int[] pixels) {
		saveScreenshot(w,h,pixels,false);
	}

	/**
	 * TODO: Convert BufferedImage Save -> STBIImageWrite Save Function
	 * @param w the width of the pixel data
	 * @param h the height of the pixel data
	 * @param pixels the pixel data (size = w * h)
	 */
	public static void saveScreenshot(int w, int h,int[] pixels,boolean flag) {
		if(flag) {
			Date d = new Date();
			Random rand = new Random();
			DateFormat format = DateFormat.getDateTimeInstance();
			File f = new File(ScreenshotsDir, "screenshot " + format.format(d).replace(':', '-') + "-" + rand.nextInt(99) + ".png");
			ByteBuffer DATA = MemoryUtil.memAlloc(4 * w * h);
			DATA.asIntBuffer().put(pixels);
			DATA.position(0);
			STBImageWrite.stbi_write_png(f.getAbsolutePath(),w,h,4,DATA,0);
		}else {
			BufferedImage IMG = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);//Use BufferedImage for ImageIO
			for (int x = 0; x < w; x++) {//Set with Y-Invert
				for (int y = 0; y < h; y++) {
					IMG.setRGB(x, h - y - 1, pixels[y * w + x]);
				}
			}
			try {
				Date d = new Date();
				Random rand = new Random();
				DateFormat format = DateFormat.getDateTimeInstance();
				ImageIO.write(IMG, "PNG", new File(ScreenshotsDir, "screenshot" + format.format(d).replace(':', '-') + "-" + rand.nextInt(99) + ".png"));
			} catch (IOException e) {
				Logger.getLogger("Screenshots").Severe("Failed to Create New Screenshot");
			}
		}
	}

	public static List<String> listAllGameSaves() {
		File[] subFiles = SaveDir.listFiles();
		List<String> saves = new ArrayList<>();
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
