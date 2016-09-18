package net.openvoxel.client.gui_framework;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by James on 10/09/2016.
 *
 * Slide Bar
 */
public class GUISlider extends GUIObjectSizable{

	private int minVal;
	private int maxVal;
	private int currentVal;
	private boolean sliderSelected = false;
	private Function<Integer,String> txtFunc;
	private static ResourceHandle texBackground = ResourceManager.getImage("gui/scrollbg0");
	private static ResourceHandle texBar = ResourceManager.getImage("gui/scrollbar0");
	private float scrollbarSizePerc = 10.0F;

	private BiConsumer<GUISlider,Integer> updateSliderFunc;

	public int getValue() {
		return currentVal;
	}

	public GUISlider(int min, int max, int current,Function<Integer,String> getText) {
		minVal = min;
		maxVal = max;
		currentVal = current >= minVal ? current <= maxVal ? current : maxVal : minVal;
		txtFunc = getText;
	}

	public void setUpdateFunc(BiConsumer<GUISlider,Integer> update) {
		updateSliderFunc = update;
	}

	@Override
	public void Draw(GUIRenderer.GUITessellator drawHandle) {
		//Draw Background//
		DrawSquare(drawHandle,texBackground);
		//Draw Handle//
		if(sliderSelected) {
			DrawBar(drawHandle, 0xFFFFDD55);
		}else{
			DrawBar(drawHandle, 0xFFFFFFFF);
		}
		//Draw Text//
		if(txtFunc != null) {
			String val = txtFunc.apply(currentVal);
			float X = getPosX(drawHandle.getScreenWidth());
			float Y = getPosY(drawHandle.getScreenHeight());
			float H = getHeight(drawHandle.getScreenHeight()) * 2;
			float W = getWidth(drawHandle.getScreenWidth());
			float TXT_W = drawHandle.GetTextWidthRatio(val) * H;
			float W_OFF = W - (TXT_W/2);
			X = X * 2 - 1 + W_OFF;
			Y = -Y * 2 + 1 - H;
			drawHandle.DrawText(X,Y,H,val);
		}
	}

	public float getScrollbarX(float width) {
		float Perc = (float)(currentVal - minVal) / (float)(maxVal - minVal);
		float x = getPosX(width);
		float w = getWidth(width);
		float scrollbarWidth = w / scrollbarSizePerc;
		float offset = Perc * (w - scrollbarWidth);
		return x + offset;
	}
	public float getScrollbarWidth(float width) {
		return getWidth(width) / scrollbarSizePerc;
	}

	public void DrawBar(GUIRenderer.GUITessellator drawHandle,int col) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getScrollbarX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getScrollbarWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin();
		drawHandle.EnableColour(true);
		drawHandle.EnableTexture(true);
		drawHandle.SetTexture(texBar);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y2,0,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.VertexWithColUV(X2,Y1,1,0,col);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.Draw();
	}

	@Override
	public void OnMouseMove(float newX, float newY, float oldX, float oldY) {
		final float X1 = getScrollbarX(ClientInput.currentWindowWidth);
		//final float Y1 = getPosY(ClientInput.currentWindowHeight);
		final float X2 = X1 + getScrollbarWidth(ClientInput.currentWindowWidth);
		//final float Y2 = Y1 + getHeight(ClientInput.currentWindowHeight);
		if(sliderSelected) {
			float hW = (X2 - X1) / 2.0F;
			float minX = getPosX(ClientInput.currentWindowWidth) + hW;
			float maxX = getPosX(ClientInput.currentWindowWidth)+getWidth(ClientInput.currentWindowWidth) - hW;
			float perc = (newX - minX) / (maxX - minX);
			System.out.println("Per="+perc);
			if(perc > 1.0F) {
				perc = 1.0F;
			}else if(perc < 0.0F) {
				perc = 0.0F;
			}
			int newID = minVal + (int)(perc * (maxVal - minVal));
			currentVal = newID;
			if(updateSliderFunc != null) {
				updateSliderFunc.accept(this,currentVal);
			}
		}
	}

	@Override
	public void OnMousePress(double x, double y) {
		final float X1 = getScrollbarX(ClientInput.currentWindowWidth);
		final float Y1 = getPosY(ClientInput.currentWindowHeight);
		final float X2 = X1 + getScrollbarWidth(ClientInput.currentWindowWidth);
		final float Y2 = Y1 + getHeight(ClientInput.currentWindowHeight);
		if(x >= X1 && x <= X2 && y >= Y1 && y <= Y2) {
			onMouseClicked();
		}
	}

	@Override
	public void OnMouseRelease(double x, double y) {
		onMouseReleased();
	}

	@Override
	public void onMouseClicked() {
		sliderSelected = true;
	}

	@Override
	public void onMouseReleased() {
		sliderSelected = false;
	}
}
