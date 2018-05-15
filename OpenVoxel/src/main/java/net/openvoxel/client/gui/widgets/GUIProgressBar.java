package net.openvoxel.client.gui.widgets;

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

	public GUIProgressBar(boolean displayPercent) {
		maxVal = 1;
		progressVal = 0;
		this.displayPercent = displayPercent;
	}

	public void setMaxVal(int max) {
		this.maxVal = max;
	}

	public void setCurrent(int val) {
		progressVal = val;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin(null);
		drawHandle.VertexWithColUV(X2,Y2,1,1,0xFFFFFFFF);
		drawHandle.VertexWithColUV(X1,Y2,0,1,0xFFFFFFFF);
		drawHandle.VertexWithColUV(X1,Y1,0,0,0xFFFFFFFF);
		drawHandle.VertexWithColUV(X2,Y1,1,0,0xFFFFFFFF);
		drawHandle.VertexWithColUV(X2,Y2,1,1,0xFFFFFFFF);
		drawHandle.VertexWithColUV(X1,Y1,0,0,0xFFFFFFFF);
		//Internal//
		final float dX = 0.5F / screenWidth;
		final float dY = 0.5F / screenHeight;
		final float XI1 = X1 + dX;
		float perc = (float) progressVal / (float) maxVal;
		float XI2_max = X2 - dX;
		final float XI2 = (XI1 * (1-perc)) + (XI2_max * perc);
		final float YI1 = Y1 + dY;
		final float YI2 = Y2 - dY;
		drawHandle.VertexWithColUV(XI2,YI2,1,1,0xFFAAAAAA);
		drawHandle.VertexWithColUV(XI1,YI2,0,1,0xFFAAAAAA);
		drawHandle.VertexWithColUV(XI1,YI1,0,0,0xFFAAAAAA);
		drawHandle.VertexWithColUV(XI2,YI1,1,0,0xFFAAAAAA);
		drawHandle.VertexWithColUV(XI2,YI2,1,1,0xFFAAAAAA);
		drawHandle.VertexWithColUV(XI1,YI1,0,0,0xFFAAAAAA);
		//Text
		String text;
		if(displayPercent) {
			int percI = (maxVal * 100) / progressVal;
			text = percI + "%";
		}else{
			text = progressVal + "/" + maxVal;
		}
		float diff = drawHandle.GetTextWidthRatio(text);
		float H = (Y2 - Y1) * 1.25F;
		float X = ((X1 + X2) / 2) * 2 - 1 - (H * diff / 2);
		float Y = -Y2 * 2 + 1 + (H / 2);
		drawHandle.DrawText(X,Y,H,text,0xFF000000);
		//drawHandle.DrawText(X,Y,H,text,0xFF000000,0xFF000000);
	}

}
