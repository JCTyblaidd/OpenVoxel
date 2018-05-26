package net.openvoxel.client.gui.util;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.gui.framework.Screen;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.server.ClientServer;
import net.openvoxel.statistics.SystemStatistics;

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
	private StringBuilder text_builder = new StringBuilder(128);
	private String open_voxel_version_str = "Open Voxel " + OpenVoxel.currentVersion.getValString();

	private int worldViewChunkRate;
	private int worldShadowChunkRate;
	private int worldNearbyChunkRate;
	private int worldUpdateChunkRate;

	public void setFrameRate(float frame_rate) {
		this.frame_rate = frame_rate;
	}

	public void setDrawInfo(int view, int shadow, int near, int update) {
		worldViewChunkRate = view;
		worldShadowChunkRate = shadow;
		worldNearbyChunkRate = near;
		worldUpdateChunkRate = update;
	}

	private void clear_text() {
		text_builder.delete(0,text_builder.length());
	}

	private StringBuilder _limit(float f) {
		clear_text();
		int frame_val = Math.round(f);
		//if(frame_val < 100) text_builder.append(' ');
		if(frame_val < 10) text_builder.append('0');
		text_builder.append(frame_val);
		text_builder.append(" fps");
		return text_builder;
	}

	private StringBuilder _memory(String prefix,long value) {
		clear_text();
		text_builder.append(prefix);
		if(value >= 1073741824) {
			long div = value / 1073741824;
			long rem = value - (div * 1073741824);
			float FV = div + (rem / 1073741824.0F);
			float VAL = Math.round(FV * 10.0F) / 10.0F;
			text_builder.append(VAL);
			text_builder.append("GB");
		}else if(value >= (1048576)) {
			long div = value / 1048576;
			long rem = value - (div * 1048576);
			float FV = div + (rem / 1048576.0F);
			float VAL = Math.round(FV * 10.0F) / 10.0F;
			text_builder.append(VAL);
			text_builder.append("MB");
		}else if(value >= (1024)) {
			long div = value / 1024;
			long rem = value - (div * 1024);
			float FV = div + (rem / 1024.0F);
			float VAL = Math.round(FV * 10.0F) / 10.0F;
			text_builder.append(VAL);
			text_builder.append("KB");
		}else {
			text_builder.append(value);
			text_builder.append("B");
		}
		return text_builder;
	}

	private StringBuilder _percent(String prefix,double percent) {
		clear_text();
		text_builder.append(prefix);
		int value = (int)Math.floor(percent * 100);
		int remainder = (int)Math.round(percent * 10000) % 100;
		text_builder.append(value);
		text_builder.append('.');
		if(remainder < 10) text_builder.append('0');
		text_builder.append(remainder);
		text_builder.append('%');
		return text_builder;
	}

	private StringBuilder _count(String prefix,int value) {
		clear_text();
		text_builder.append(prefix);
		text_builder.append(value);
		return text_builder;
	}

	private void _1dp(float val) {
		text_builder.append((int)val);
		text_builder.append('.');
		final int decimal = Math.abs((int)(val * 10.F) % 10);
		text_builder.append(decimal);
	}

	private StringBuilder _simpleFloat(String prefix, float val) {
		clear_text();
		text_builder.append(prefix);
		_1dp(val);
		return text_builder;
	}

	private StringBuilder _vec3f(String prefix, float x, float y, float z) {
		clear_text();
		text_builder.append(prefix);
		text_builder.append('(');
		_1dp(x);
		text_builder.append(',');
		_1dp(y);
		text_builder.append(',');
		_1dp(z);
		text_builder.append(')');
		return text_builder;
	}

	private StringBuilder _3int(String prefix, int x, int y, int z) {
		clear_text();
		text_builder.append(prefix);
		text_builder.append(x);
		text_builder.append('/');
		text_builder.append(y);
		text_builder.append('/');
		text_builder.append(z);
		return text_builder;
	}

	@Override
	public void DrawScreen(IGuiRenderer tess) {
		SystemStatistics.requestUpdate();
		int debug = debugLevel.getVal();
		float screenHeight = tess.getScreenHeight();
		float screenWidth = tess.getScreenWidth();
		float height = 25.0F / screenHeight;
		final float x_pos = 5.F / screenWidth;
		float y_pos = height;
		float y_pos2 = height;
		if(debug > 1) {
			tess.DrawText(x_pos,y_pos,height,open_voxel_version_str);
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
			y_pos += height;
			final float histogram_w = 250.0F;
			final float histogram_h = 150.0F;
			draw_processor_histogram(tess,1.0F-((histogram_w+5)/screenWidth),y_pos2 + (height/4),
					histogram_w/screenWidth,histogram_h/screenHeight);
		}
		ClientServer clientServer;
		if(debug > 1 && (clientServer = OpenVoxel.getClientServer()) != null) {
			EntityPlayerSP thePlayer = clientServer.getThePlayer();
			tess.DrawText(x_pos,y_pos,height,_simpleFloat("Player Yaw: ",thePlayer.getYaw()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_simpleFloat("Player Pitch: ",thePlayer.getPitch()));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_vec3f("Player Pos: ",
					(float)thePlayer.xPos,(float)thePlayer.yPos,(float)thePlayer.zPos)
			);
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_count("Chunk Redraw Rate: ",worldUpdateChunkRate));
			y_pos += height;
			tess.DrawText(x_pos,y_pos,height,_3int("Chunk Draw Rate: ",
					worldViewChunkRate,worldShadowChunkRate,worldNearbyChunkRate));
		}
	}

	private void draw_processor_histogram(IGuiRenderer tess, float x1, float y1, float w, float h) {
		float x2 = x1 + w;
		float y2 = y1 + h;
		final int COL = 0x9B000000;    // = 0x5B000000;
		final int CPU_COL = 0xEFD39539;// = 0xCFD39539
		final int GPU_COL = 0xEF168206;// = 0xCF168206;
		final int MIX_COL = 0xEF748c20;// blend(GPU_COL,CPU_COL)
		tess.Begin(null);
		tess.VertexRect(x1,x2,y1,y2,COL);

		float diff = w / 31;
		for(int i = 1; i < 32; i++) {
			int idx = (i + SystemStatistics.write_index) % 32;
			float height_cpu = (float)(SystemStatistics.processor_history[idx] * h);
			float height_gpu = (float)(SystemStatistics.graphics_history[idx] * h);

			float min_height = height_cpu;
			float max_height = height_gpu;
			int max_col = GPU_COL;
			if(height_cpu >= height_gpu) {
				min_height = height_gpu;
				max_height = height_cpu;
				max_col = CPU_COL;
			}
			float x_pos1 = x2 - (diff * i) + diff;
			float x_pos2 = x_pos1 - diff;
			tess.VertexRect(x_pos2,x_pos1,y2-min_height,y2,MIX_COL);
			tess.VertexRect(x_pos2,x_pos1,y2-max_height,y2-min_height,max_col);
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
