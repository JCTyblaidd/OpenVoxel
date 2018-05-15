package net.openvoxel.client.gui.widgets.display;

import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;

/**
 * Created by James on 08/04/2017.
 *
 * Progress Bar
 */
public class GUIProgressBar extends GUIObjectSizable {

	private int maxVal;
	private int progressVal;
	private boolean displayPercent;
	private StringBuilder builder;

	public GUIProgressBar(boolean displayPercent) {
		maxVal = 1;
		progressVal = 0;
		this.displayPercent = displayPercent;
		builder = new StringBuilder();
	}

	public void setMaxVal(int max) {
		this.maxVal = max;
	}

	public void setCurrent(int val) {
		progressVal = val;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		DrawSquare(drawHandle,null,0xFFFFFFFF);
		float percent = (float)progressVal / (float)maxVal;
		DrawSquareScaled(drawHandle,null,0xFF0000FF,percent,1.F);
		builder.delete(0,builder.length());
		if(displayPercent) {
			int percent_int = Math.round(percent * 100.F);
			builder.append(percent_int);
			builder.append('%');
		}else{
			builder.append(progressVal);
			builder.append('/');
			builder.append(maxVal);
		}
		DrawSquareWithText(drawHandle, null, 0, builder, 0.9F);
	}

}
