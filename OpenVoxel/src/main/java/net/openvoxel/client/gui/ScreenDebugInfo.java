package net.openvoxel.client.gui;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.statistics.SystemStatistics;

import java.text.DecimalFormat;

/**
 * Created by James on 10/09/2016.
 *
 * Special Hook: for Debug Data
 */
public class ScreenDebugInfo extends Screen {

	public static final ScreenDebugInfo instance = new ScreenDebugInfo();
	public static GUIDebugLevel debugLevel = GUIDebugLevel.EXTREME_DETAIL;

	public static String RendererType   = "Unknown Renderer";
	public static String RendererVendor = "Unknown Vendor";
	public static String RendererDriver = "Unknown Driver";

	private float frame_rate = 40.0f;
	private StringBuffer text_builder = new StringBuffer(128);

	public void setFrameRate(float frame_rate) {
		this.frame_rate = frame_rate;
	}

	private void clear_text() {
		text_builder.delete(0,text_builder.length());
	}

	private StringBuffer _limit(float f) {
		clear_text();
		int frame_val = Math.round(f);
		if(frame_val < 100) text_builder.append(' ');
		if(frame_val < 10) text_builder.append(' ');
		text_builder.append(frame_val);
		text_builder.append(" fps");

		//text_builder.append(Math.round(f));

		/*
		String val = Integer.toString(Math.round(f));
		int res = val.length();
		StringBuilder build = new StringBuilder();
		res = 3 - res;
		for(int i = 0; i < res; i++) {
			build.append(' ');
		}
		build.append(val);
		build.append(" fps");
		Logger.getLogger("DEBUG").Info("|",build.toString(),"||",text_builder.toString(),"|");
		*/

		return text_builder;
	}

	private StringBuffer _memory(String prefix,long value) {
		clear_text();
		text_builder.append(prefix);
		if(value >= 1073741824) {
			long div = value / 1073741824;
			long rem = value - (div * 1073741824);
			float FV = div + (rem / 1073741824.0F);
			float VAL = Math.round(FV * 10.0F) / 10.0F;
			text_builder.append(VAL);
			text_builder.append("GB");
			//val =  Float.toString(Math.round(FV * 10.0F) / 10.0F) + "GB";
		}else if(value >= (1048576)) {
			long div = value / 1048576;
			long rem = value - (div * 1048576);
			float FV = div + (rem / 1048576.0F);
			float VAL = Math.round(FV * 10.0F) / 10.0F;
			text_builder.append(VAL);
			text_builder.append("MB");
			//val =  Float.toString(Math.round(FV * 10.0F) / 10.0F) + "MB";
		}else if(value >= (1024)) {
			long div = value / 1024;
			long rem = value - (div * 1024);
			float FV = div + (rem / 1024.0F);
			float VAL = Math.round(FV * 10.0F) / 10.0F;
			text_builder.append(VAL);
			text_builder.append("KB");
			//val =  Float.toString(Math.round(FV * 10.0F) / 10.0F) + "KB";
		}else {
			text_builder.append(value);
			text_builder.append("B");
			//val =  Long.toString(value)+"B";
		}
		return text_builder;
	}

	private StringBuffer _percent(String prefix,double percent) {
		clear_text();
		text_builder.append(prefix);
		int value = (int)Math.floor(percent * 100);
		int remainder = (int)Math.round(percent * 10000) % 100;
		text_builder.append(value);
		text_builder.append('.');
		if(remainder < 10) text_builder.append('0');
		text_builder.append(remainder);
		text_builder.append('%');

/*
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(2);
		decimalFormat.setMinimumFractionDigits(2);
		String cmp =  prefix + decimalFormat.format(percent*100) + '%';
		Logger.getLogger("debug").Info(percent,"::",cmp," :: ",text_builder.toString());
*/
		return text_builder;
	}

	private StringBuffer _count(String prefix,int value) {
		clear_text();
		text_builder.append(prefix);
		text_builder.append(value);
		return text_builder;
		//return prefix + value;
	}

	@Override
	public void DrawScreen(IGuiRenderer tess) {
		SystemStatistics.requestUpdate();
		int debug = debugLevel.getVal();
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
			CharSequence str = _limit(frame_rate);
			tess.DrawText(x_pos,y_pos,height,str);
			y_pos += height;
		}
		if(debug > 1) {//At Least Level::FPS_BONUS
			//RUNTIME INFO//
			tess.DrawText(x_pos,y_pos,height,_memory("Process memory: ",SystemStatistics.getProcessMemoryUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_memory("JVM memory: ",SystemStatistics.getJVMMemoryUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_count("Processor Count: ",SystemStatistics.getProcessorCount()));
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
			tess.DrawText(x_pos,y_pos,height,_percent("Processor Usage: ",SystemStatistics.getProcessingUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_count("Thread Count: ",SystemStatistics.getThreadCount()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_percent("GPU Usage: ",SystemStatistics.getGraphicsProcessingUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_memory("GPU Memory Usage: ",SystemStatistics.getGraphicsGpuMemoryUsage()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_memory("GPU-Shared Memory Usage: ",SystemStatistics.getGraphicsLocalMemoryUsage()));
			final float histogram_w = 250.0F;
			final float histogram_h = 150.0F;
			draw_processor_histogram(tess,1.0F-((histogram_w+5)/screenWidth),y_pos2 + (height/4),
					histogram_w/screenWidth,histogram_h/screenHeight);
		}
	}

	private void draw_processor_histogram(IGuiRenderer tess, float x1, float y1, float w, float h) {
		float x2 = x1 + w;
		float y2 = y1 + h;
		final int COL = 0x9B000000;    // = 0x5B000000;
		final int CPU_COL = 0xEFD39539;// = 0xCFD39539
		final int GPU_COL = 0xEF168206;// = 0xCF168206;
		tess.Begin(null);
		tess.VertexWithCol(x2,y2,COL);
		tess.VertexWithCol(x1,y2,COL);
		tess.VertexWithCol(x1,y1,COL);
		tess.VertexWithCol(x2,y1,COL);
		tess.VertexWithCol(x2,y2,COL);
		tess.VertexWithCol(x1,y1,COL);
		//Draw Histogram//
		float diff = w / 30;
		for(int i = 1; i < 31; i++) {
			int j1 = (i + SystemStatistics.write_index) % 32;
			int j2 = (i + 1 + SystemStatistics.write_index) % 32;
			float y_val1 = (float)(SystemStatistics.processor_history[j1] * h);
			float y_val2 = (float)(SystemStatistics.processor_history[j2] * h);
			float y_pos1 = y2 - y_val1;
			float y_pos2 = y2 - y_val2;
			float x_pos1 = x2 - (diff * i) + diff;
			float x_pos2 = x_pos1 - diff;
			tess.VertexWithCol(x_pos2,y2,CPU_COL);
			tess.VertexWithCol(x_pos1,y_pos1,CPU_COL);
			tess.VertexWithCol(x_pos1,y2,CPU_COL);

			tess.VertexWithCol(x_pos2,y2,CPU_COL);
			tess.VertexWithCol(x_pos2,y_pos2,CPU_COL);
			tess.VertexWithCol(x_pos1,y_pos1,CPU_COL);
		}
		for(int i = 1; i < 31; i++) {
			int j1 = (i + SystemStatistics.write_index) % 32;
			int j2 = (i + 1 + SystemStatistics.write_index) % 32;
			float y_val1 = (float)(SystemStatistics.graphics_history[j1] * h);
			float y_val2 = (float)(SystemStatistics.graphics_history[j2] * h);
			float y_pos1 = y2 - y_val1;
			float y_pos2 = y2 - y_val2;
			float x_pos1 = x2 - (diff * i) + diff;
			float x_pos2 = x_pos1 - diff;
			tess.VertexWithCol(x_pos2,y2,GPU_COL);
			tess.VertexWithCol(x_pos1,y_pos1,GPU_COL);
			tess.VertexWithCol(x_pos1,y2,GPU_COL);

			tess.VertexWithCol(x_pos2,y2,GPU_COL);
			tess.VertexWithCol(x_pos2,y_pos2,GPU_COL);
			tess.VertexWithCol(x_pos1,y_pos1,GPU_COL);
		}
	}

	private void draw_memory_piechart(IGuiRenderer tess, float x1, float y1, float w, float h) {
		final float JVM = SystemStatistics.getJVMMemoryUsage();
		final float ALLOC = SystemStatistics.getProcessMemoryUsage();
		final float GPU = SystemStatistics.getGraphicsLocalMemoryUsage();

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
