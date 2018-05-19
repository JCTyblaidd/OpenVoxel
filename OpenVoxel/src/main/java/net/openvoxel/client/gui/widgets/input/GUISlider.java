package net.openvoxel.client.gui.widgets.input;

import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by James on 10/09/2016.
 *
 * Slide Bar
 */
public class GUISlider extends GUIObjectSizable {

	private static ResourceHandle texBackground = ResourceManager.getImage("gui/scrollbg0");
	private static ResourceHandle texBar = ResourceManager.getImage("gui/scrollbar0");

	private boolean sliderSelected = false;
	private float scrollbarSizePercent = 10.0F;
	private boolean drawDirtyFlag = false;

	private int minVal;
	private int maxVal;
	private int currentVal;
	private StringBuilder txt_builder = new StringBuilder();

	private TextUpdateFunctor txtFunc;
	private BiConsumer<GUISlider,Integer> updateSliderFunc;

	public interface TextUpdateFunctor {
		void generate(StringBuilder builder, int percent);
	}

	public int getValue() {
		return currentVal;
	}

	public GUISlider(int min, int max, int current,TextUpdateFunctor getText) {
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
			txt_builder.delete(0,txt_builder.length());
			txtFunc.generate(txt_builder,currentVal);
			DrawSquareWithText(drawHandle,null,0,txt_builder,0.95F);
			//TODO: MAKE TEXT SIZE MORE STABLE!!!
		}
	}

	private float getScrollbarX(float width) {
		float percent = (float)(currentVal - minVal) / (float)(maxVal - minVal);
		float x = getPosX(width);
		float w = getWidth(width);
		float scrollbarWidth = w / scrollbarSizePercent;
		float offset = percent * (w - scrollbarWidth);
		return x + offset;
	}
	private float getScrollbarWidth(float width) {
		return getWidth(width) / scrollbarSizePercent;
	}

	private void DrawBar(IGuiRenderer drawHandle, int col) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getScrollbarX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getScrollbarWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin(texBar);
		drawHandle.VertexRect(X1,X2,Y1,Y2,col);
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
