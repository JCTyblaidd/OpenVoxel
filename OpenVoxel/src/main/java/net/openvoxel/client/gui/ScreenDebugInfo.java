package net.openvoxel.client.gui;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.control.RenderThread;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 10/09/2016.
 *
 * Special Hook: for Debug Data
 */
public class ScreenDebugInfo extends Screen{

	public static final ScreenDebugInfo instance = new ScreenDebugInfo();
	public static volatile GUIDebugLevel debugLevel = GUIDebugLevel.EXTREME_DETAIL;

	private static String _limit(float f) {
		//return Float.toString(Math.round(f * 10.0F) / 10.0F);
		String val = Integer.toString(Math.round(f));
		int res = val.length();
		StringBuilder build = new StringBuilder();
		res = 4 - res;
		for(int i = 0; i < res; i++) {
			build.append(' ');
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

	@Override
	public void DrawScreen(GUIRenderer.GUITessellator tess) {
		int debug = debugLevel.getVal();
		int h = ClientInput.currentWindowHeight;
		int w = ClientInput.currentWindowWidth;
		float height = 50.0F / h;
		float x_pos = -1.0F;
		float y_pos = 1.0F- height;
		if(debug > 0) {//At Least Level::FPS
			float val = RenderThread.getFrameRate();
			String str = _limit(val);
			tess.DrawText(x_pos,y_pos,height,str);
		}
		if(debug > 1) {//At Least Level::FPS_BONUS
			Runtime runtime = Runtime.getRuntime();
			y_pos -= height;
			long totalMem = runtime.totalMemory();
			tess.DrawText(x_pos,y_pos,height,"Total memory: " + _memory(totalMem));
			y_pos -= height;
			tess.DrawText(x_pos,y_pos,height,"Used memory: " + _memory(totalMem-runtime.freeMemory()));
			y_pos -= height;
			tess.DrawText(x_pos,y_pos,height,"Processor Count: " + runtime.availableProcessors());
		}
		if(debug > 2) {//Contains Extreme Detail

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
