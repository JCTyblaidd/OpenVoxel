package net.openvoxel.client.gui.widgets;

import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by James on 10/09/2016.
 *
 * Slide Bar
 */
public class GUISlider extends GUIObjectSizable {

	private int minVal;
	private int maxVal;
	private int currentVal;
	private boolean sliderSelected = false;
	private Function<Integer,String> txtFunc;
	private static ResourceHandle texBackground = ResourceManager.getImage("gui/scrollbg0");
	private static ResourceHandle texBar = ResourceManager.getImage("gui/scrollbar0");
	private float scrollbarSizePerc = 10.0F;
	private boolean drawDirtyFlag = false;

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

	public void setUpdateFunc(Consumer<Integer> update) {
		setUpdateFunc((slider,val) -> update.accept(val));
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
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
			float H = getHeight(drawHandle.getScreenHeight());
			float W = getWidth(drawHandle.getScreenWidth());
			float TXT_W = drawHandle.GetTextWidthRatio(val) * H;
			X += (W / 2) - (TXT_W/2);
			Y += H;
			drawHandle.DrawText(X,Y,H,val);
		}
	}

	private float getScrollbarX(float width) {
		float Perc = (float)(currentVal - minVal) / (float)(maxVal - minVal);
		float x = getPosX(width);
		float w = getWidth(width);
		float scrollbarWidth = w / scrollbarSizePerc;
		float offset = Perc * (w - scrollbarWidth);
		return x + offset;
	}
	private float getScrollbarWidth(float width) {
		return getWidth(width) / scrollbarSizePerc;
	}

	private void DrawBar(IGuiRenderer drawHandle, int col) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getScrollbarX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getScrollbarWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin(texBar);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y2,0,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.VertexWithColUV(X2,Y1,1,0,col);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
	}

	@Override
	public void OnMouseMove(float newX, float newY, float oldX, float oldY,float screenWidth, float screenHeight) {
		final float X1 = getScrollbarX(screenWidth);
		final float X2 = X1 + getScrollbarWidth(screenWidth);
		if(sliderSelected) {
			float hW = (X2 - X1) / 2.0F;
			float minX = getPosX(screenWidth) + hW;
			float maxX = getPosX(screenWidth)+getWidth(screenWidth) - hW;
			float perc = (newX - minX) / (maxX - minX);
			if(perc > 1.0F) {
				perc = 1.0F;
			}else if(perc < 0.0F) {
				perc = 0.0F;
			}
			currentVal = minVal + (int)(perc * (maxVal - minVal));
			if(updateSliderFunc != null) {
				updateSliderFunc.accept(this,currentVal);
			}
			drawDirtyFlag = true;
		}
	}

	@Override
	public void OnMousePress(float x, float y, float screenWidth, float screenHeight) {
		final float X1 = getScrollbarX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getScrollbarWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		if(x >= X1 && x <= X2 && y >= Y1 && y <= Y2) {
			onMouseClicked();
		}
	}

	@Override
	public void OnMouseRelease(float x, float y,float screenWidth, float screenHeight) {
		onMouseReleased();
	}

	@Override
	public void onMouseClicked() {
		sliderSelected = true;
		drawDirtyFlag = true;
	}

	@Override
	public void onMouseReleased() {
		sliderSelected = false;
		drawDirtyFlag = true;
	}

	@Override
	public boolean isDrawDirty() {
		boolean tmp = drawDirtyFlag;
		drawDirtyFlag = false;
		return tmp;
	}
}
