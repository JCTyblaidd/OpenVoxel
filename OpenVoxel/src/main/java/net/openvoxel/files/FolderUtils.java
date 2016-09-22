package net.openvoxel.files;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

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
		f.mkdir();
		return f;
	}

	/**
	 * TODO: Convert BufferedImage Save -> STBIImageWrite Save Function
	 * @param w the width of the pixel data
	 * @param h the height of the pixel data
	 * @param pixels the pixel data (size = w * h)
	 */
	public static void saveScreenshot(int w, int h,int[] pixels) {
		BufferedImage IMG = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);//Use BufferedImage for ImageIO
		for (int x = 0; x < w; x++) {//Set with Y-Invert
			for (int y = 0; y < h; y++) {
				IMG.setRGB(x,h-y-1, pixels[y * w + x]);
			}
		}
		try {
			Date d = new Date();
			DateFormat format = DateFormat.getDateTimeInstance();
			ImageIO.write(IMG, "PNG", new File(ScreenshotsDir, "screenshot" + format.format(d).replace(':','-') +".png"));
		}catch(IOException e) {
			//TODO: Handle
		}
	}


}
