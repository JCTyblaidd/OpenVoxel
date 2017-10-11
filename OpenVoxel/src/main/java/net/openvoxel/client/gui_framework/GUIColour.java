package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 11/09/2016.
 */
public class GUIColour extends GUIObjectSizable{

	private int Col_ll;
	private int Col_lm;
	private int Col_ml;
	private int Col_mm;

	public GUIColour(int col) {
		this(col,col,col,col);
	}
	public GUIColour(int min_min, int min_max, int max_min, int max_max) {
		Col_ll = min_min;
		Col_lm = min_max;
		Col_ml = max_min;
		Col_mm = max_max;
	}

	public GUIColour(int min, int max,boolean isHorizontal) {
		this(min,isHorizontal ? min : max,isHorizontal ? max : min,max);
	}

	@Override
	public void Draw(GUIRenderer.GUITessellator drawHandle) {
		DrawSquare(drawHandle);
	}

	public void DrawSquare(GUIRenderer.GUITessellator drawHandle) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin();
		drawHandle.EnableTexture(false);

		drawHandle.VertexWithColUV(X2,Y2,1,1,Col_mm);
		drawHandle.VertexWithColUV(X1,Y2,0,1,Col_lm);
		drawHandle.VertexWithColUV(X1,Y1,0,0,Col_ll);
		drawHandle.VertexWithColUV(X2,Y1,1,0,Col_ml);
		drawHandle.VertexWithColUV(X2,Y2,1,1,Col_mm);
		drawHandle.VertexWithColUV(X1,Y1,0,0,Col_ll);
		drawHandle.Draw();
	}
}
