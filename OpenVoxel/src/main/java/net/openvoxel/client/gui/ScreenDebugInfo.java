package net.openvoxel.client.gui;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.control.RenderThread;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.statistics.SystemStatistics;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by James on 10/09/2016.
 *
 * Special Hook: for Debug Data
 */
public class ScreenDebugInfo extends Screen{

	public static final ScreenDebugInfo instance = new ScreenDebugInfo();
	public static AtomicReference<GUIDebugLevel> debugLevel = new AtomicReference<>(GUIDebugLevel.EXTREME_DETAIL);

	public static String RendererType   = "Unknown Renderer";
	public static String RendererVendor = "Unknown Vendor";
	public static String RendererDriver = "Unknown Driver";


	private static String _limit(float f) {
		//return Float.toString(Math.round(f * 10.0F) / 10.0F);
		String val = Integer.toString(Math.round(f));
		int res = val.length();
		StringBuilder build = new StringBuilder();
		res = 3 - res;
		for(int i = 0; i < res; i++) {
			build.append(' ');
		}
		build.append(val);
		build.append(" fps");
		return build.toString();
	}

	private static String _memory(long value) {
		if(value >= 1073741824) {
			long div = value / 1073741824;
			long rem = value - (div * 1073741824);
			float FV = div + (rem / 1073741824.0F);
			return Float.toString(Math.round(FV * 10.0F) / 10.0F) + "GB";
		}else if(value >= (1048576)) {
			long div = value / 1048576;
			long rem = value - (div * 1048576);
			float FV = div + (rem / 1048576.0F);
			return Float.toString(Math.round(FV * 10.0F) / 10.0F) + "MB";
		}else if(value >= (1024)) {
			long div = value / 1024;
			long rem = value - (div * 1024);
			float FV = div + (rem / 1024.0F);
			return Float.toString(Math.round(FV * 10.0F) / 10.0F) + "KB";
		}else {
			return Long.toString(value)+"B";
		}
	}

	private static String _perc(double percent) {
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(2);
		decimalFormat.setMinimumFractionDigits(2);
		return decimalFormat.format(percent) + '%';
	}

	@Override
	public void DrawScreen(GUIRenderer.GUITessellator tess) {
		SystemStatistics.requestUpdate();
		int debug = debugLevel.get().getVal();
		int h = ClientInput.currentWindowHeight.get();
		//int w = ClientInput.currentWindowWidth;
		float height = 50.0F / h;
		final float x_pos = -1.0F;
		float y_pos = 1.0F- height;
		float y_pos2 = 1.0F - height;
		if(debug > 1) {
			tess.DrawText(x_pos,y_pos,height,"Open Voxel " + OpenVoxel.currentVersion.getValString());
			y_pos -= height;
		}
		if(debug > 0) {//At Least Level::FPS
			float val = RenderThread.getFrameRate();
			String str = _limit(val);
			tess.DrawText(x_pos,y_pos,height,str);
			y_pos -= height;
		}
		if(debug > 1) {//At Least Level::FPS_BONUS
			//RUNTIME INFO//
			tess.DrawText(x_pos,y_pos,height,"Process memory: " + _memory(SystemStatistics.getProcessMemoryUsage()));
			y_pos -= height;
			tess.DrawText(x_pos,y_pos,height,"JVM memory: " + _memory(SystemStatistics.getJVMMemoryUsage()));
			y_pos -= height;
			tess.DrawText(x_pos,y_pos,height,"Processor Count: " + SystemStatistics.getProcessorCount());
			y_pos -= height;
			//RENDERER INFO//
			float width1 = tess.GetTextWidthRatio(RendererType) * height;
			float width2 = tess.GetTextWidthRatio(RendererVendor) * height;
			float width3 = tess.GetTextWidthRatio(RendererDriver) * height;
			tess.DrawText(1.0F-width1,y_pos2,height,RendererType);
			y_pos2 -= height;
			tess.DrawText(1.0F-width2,y_pos2,height,RendererVendor);
			y_pos2 -= height;
			tess.DrawText(1.0F-width3,y_pos2,height,RendererDriver);
			y_pos2 -= height;
		}
		if(debug > 2) {//Contains Extreme Detail
			tess.DrawText(x_pos,y_pos,height,"Processor Usage: " +_perc(SystemStatistics.getProcessingUsage()));
			y_pos -= height;
			tess.DrawText(x_pos,y_pos,height,"Thread Count: " + SystemStatistics.getThreadCount());
			y_pos -= height;
		}
	}

	public enum GUIDebugLevel {
		NONE(0),
		FPS(1),
		FPS_BONUS(2),
		EXTREME_DETAIL(3);
		private int val;
		GUIDebugLevel(int v) {
			val = v;
		}
		public int getVal() {
			return val;
		}
	}

}
