package net.openvoxel.client.gui;

import net.openvoxel.OpenVoxel;
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

	private static String _percent(double percent) {
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(2);
		decimalFormat.setMinimumFractionDigits(2);
		return decimalFormat.format(percent*100) + '%';
	}

	@Override
	public void DrawScreen(GUIRenderer.GUITessellator tess) {
		SystemStatistics.requestUpdate();
		int debug = debugLevel.get().getVal();
		float screenHeight = tess.getScreenHeight();
		float screenWidth = tess.getScreenWidth();
		float height = 25.0F / screenHeight;
		final float x_pos = 0.0f;
		float y_pos = height;
		float y_pos2 = height;
		if(debug > 1) {
			tess.DrawText(x_pos,y_pos,height,"Open Voxel " + OpenVoxel.currentVersion.getValString());
			y_pos += height;
		}
		if(debug > 0) {//At Least Level::FPS
			float val = RenderThread.getFrameRate();
			String str = _limit(val);
			tess.DrawText(x_pos,y_pos,height,str);
			y_pos += height;
		}
		if(debug > 1) {//At Least Level::FPS_BONUS
			//RUNTIME INFO//
			tess.DrawText(x_pos,y_pos,height,"Process memory: " + _memory(SystemStatistics.getProcessMemoryUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,"JVM memory: " + _memory(SystemStatistics.getJVMMemoryUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,"Processor Count: " + SystemStatistics.getProcessorCount());
			y_pos += height;
			//RENDERER INFO//
			float width1 = tess.GetTextWidthRatio(RendererType) * height;
			float width2 = tess.GetTextWidthRatio(RendererVendor) * height;
			float width3 = tess.GetTextWidthRatio(RendererDriver) * height;
			tess.DrawText(1.0F-width1,y_pos2,height,RendererType);
			y_pos2 += height;
			tess.DrawText(1.0F-width2,y_pos2,height,RendererVendor);
			y_pos2 += height;
			tess.DrawText(1.0F-width3,y_pos2,height,RendererDriver);
		}
		if(debug > 2) {//Contains Extreme Detail
			tess.DrawText(x_pos,y_pos,height,"Processor Usage: " + _percent(SystemStatistics.getProcessingUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,"Thread Count: " + SystemStatistics.getThreadCount());
			final float histogram_w = 250.0F;
			final float histogram_h = 150.0F;
			draw_processor_histogram(tess,1.0F-((histogram_w+5)/screenWidth),y_pos2 + (height/4),
					histogram_w/screenWidth,histogram_h/screenHeight);
		}
	}

	private void draw_processor_histogram(GUIRenderer.GUITessellator tess,float x1, float y1, float w, float h) {
		float x2 = x1 + w;
		float y2 = y1 + h;
		final int COL = 0x5B000000;
		final int CPU_COL = 0xCFD39539;
		final int GPU_COL = 0xCF168206;
		tess.Begin();
		tess.VertexWithCol(x2,y2,COL);
		tess.VertexWithCol(x1,y2,COL);
		tess.VertexWithCol(x1,y1,COL);
		tess.VertexWithCol(x2,y1,COL);
		tess.VertexWithCol(x2,y2,COL);
		tess.VertexWithCol(x1,y1,COL);
		//Draw Histogram//
		float diff = w / 32;
		for(int i = 0; i < 32; i++) {
			int j1 = (i + SystemStatistics.write_index) % 32;
			int j2 = (i + 1 + SystemStatistics.write_index) % 32;
			float y_val1 = (float)(SystemStatistics.processor_history[j1] * h);
			float y_val2 = (float)(SystemStatistics.processor_history[j2] * h);
			float y_pos1 = y2 - y_val1;
			float y_pos2 = y2 - y_val2;
			float x_pos1 = x2 - (diff * i);
			float x_pos2 = x_pos1 - diff;
			tess.VertexWithCol(x_pos2,y2,CPU_COL);
			tess.VertexWithCol(x_pos1,y_pos1,CPU_COL);
			tess.VertexWithCol(x_pos1,y2,CPU_COL);

			tess.VertexWithCol(x_pos2,y2,CPU_COL);
			tess.VertexWithCol(x_pos2,y_pos2,CPU_COL);
			tess.VertexWithCol(x_pos1,y_pos1,CPU_COL);
		}
		tess.Draw();
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
