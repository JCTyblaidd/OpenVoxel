package net.openvoxel.client.gui.widgets;

import net.openvoxel.client.gui.framework.GUIObject;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 01/09/2016.
 */
public abstract class GUIObjectSizable extends GUIObject {

	public float pos_x_rel = 0;
	public float pos_y_rel = 0;
	public float pos_x_abs = 0;
	public float pos_y_abs = 0;

	public float size_w_rel = 0;
	public float size_h_rel = 0;
	public float size_w_abs = 0;
	public float size_h_abs = 0;

	public float getPosX(float width) {
		return pos_x_rel + (pos_x_abs / width);
	}
	public float getPosY(float height) {
		return pos_y_rel + (pos_y_abs / height);
	}
	public float getHeight(float height) {
		return size_h_rel + (size_h_abs / height);
	}
	public float getWidth(float width) {
		return size_w_rel + (size_w_abs / width);
	}

	public void DrawSquare(IGuiRenderer drawHandle, ResourceHandle Image) {
		DrawSquare(drawHandle,  Image, 0xFFFFFFFF);
	}

	public void DrawSquare(IGuiRenderer drawHandle, ResourceHandle Image, int col) {
		DrawSquareColoured(drawHandle,Image,col,col,col,col);
	}

	public void DrawSquareColoured(IGuiRenderer drawHandle, ResourceHandle Image, int mm, int lm, int ll, int ml) {
		DrawSquareColouredScaled(drawHandle,Image,mm,lm,ll,ml,1.F,1.F);
	}

	public void DrawSquareScaled(IGuiRenderer drawHandle, ResourceHandle Image,int col, float ws, float hs) {
		DrawSquareColouredScaled(drawHandle,Image,col,col,col,col,ws,hs);
	}

	public void DrawSquareColouredScaled(IGuiRenderer drawHandle, ResourceHandle Image, int mm, int lm, int ll, int ml, float ws, float hs) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth) * ws;
		final float Y2 = Y1 + getHeight(screenHeight) * hs;
		drawHandle.Begin(Image);
		drawHandle.VertexRect(X1,X2,Y1,Y2,mm,ml,lm,ll);
	}

	public void DrawSquareWithText(IGuiRenderer drawHandle, ResourceHandle Image, int col, CharSequence text,float widthLimit) {
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float W = getWidth(screenWidth);
		final float H = getHeight(screenHeight);
		final float X2 = X1 + W;
		final float Y2 = Y1 + H;
		if(col != 0x00000000) {
			drawHandle.Begin(Image);
			drawHandle.VertexRect(X1,X2,Y1,Y2,col);
		}
		if(text != null) {
			float TXT_RATIO = drawHandle.GetTextWidthRatio(text);
			float TXT_W = TXT_RATIO * H;
			//int col2 = 0xFF0F0FFF;
			if (TXT_W < W * widthLimit) {
				float X = X1 + (W / 2) - (TXT_W / 2);
				/*
				drawHandle.Begin(null);
				drawHandle.VertexRect(X,X+TXT,W,Y1,Y2,col2);
				*/
				drawHandle.DrawText(X, Y2, H, text);
			} else {
				float X = X1 + W * (1 - widthLimit) / 2;
				float TXT_H = W / TXT_RATIO * widthLimit;
				float Y = Y1 + TXT_H + (H - TXT_H) / 2;
				/*
				float NEW_TXT_W = TXT_RATIO * TXT_H;
				drawHandle.Begin(null);
				drawHandle.VertexRect(X,X+NEW_TXT,Y-TXT_H,Y,col2);
				*/
				drawHandle.DrawText(X, Y, TXT_H, text);
			}
		}
	}

	//
	// Location & Position API
	//

	public void setupFullscreen() {
		setPosition(0,0,0,0);
		setSize(1,1,0,0);
	}

	public void setCentered(float widthAbs, float heightAbs) {
		setupAbsSizeTargeted(0.5F,0.5F,widthAbs,heightAbs);
	}

	public void setupAbsSizeTargeted(float xRel, float yRel,float widthAbs, float heightAbs) {
		setSize(0,0,widthAbs,heightAbs);
		setPosition(xRel,yRel,-widthAbs/2,-heightAbs/2);
	}

	public void setupOffsetTo(GUIObjectSizable obj2, float xOff, float yOff, float absW, float absH) {
		setSize(0,0,absW,absH);
		setPosition(obj2.pos_x_rel,obj2.pos_y_rel,obj2.pos_x_abs+xOff,obj2.pos_y_abs+yOff);
	}

	public void setPosition(float posXRel, float posYRel, float posXAbs, float posYAbs) {
		pos_x_abs = posXAbs;
		pos_y_abs = posYAbs;
		pos_x_rel = posXRel;
		pos_y_rel = posYRel;
	}

	public void setSize(float widthRel, float heightRel, float widthAbs, float heightAbs) {
		size_h_abs = heightAbs;
		size_h_rel = heightRel;
		size_w_abs = widthAbs;
		size_w_rel = widthRel;
	}

	//
	// Enter & Exit API
	//


	protected boolean previousIn = false;
	@Override
	public void OnMouseMove(float newX, float newY, float oldX, float oldY,float screenWidth, float screenHeight) {
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		if(newX >= X1 && newX <= X2 && newY >= Y1 && newY <= Y2) {
			if(!previousIn) {
				previousIn = true;
				onMouseEnter();
			}
		}else{
			if(previousIn) {
				previousIn = false;
				onMouseLeave();
			}
		}
	}

	@Override
	public void OnMousePress(float x, float y, float screenWidth, float screenHeight) {
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		if(x >= X1 && x <= X2 && y >= Y1 && y <= Y2) {
			onMouseClicked();
		}
	}

	@Override
	public void OnMouseRelease(float x, float y, float screenWidth, float screenHeight) {
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		if(x >= X1 && x <= X2 && y >= Y1 && y <= Y2) {
			onMouseReleased();
		}
	}

	public void onMouseEnter() {
		//NO OP//
	}
	public void onMouseLeave() {
		//NO OP//
	}
	public void onMouseClicked() {
		//NO OP//
	}
	public void onMouseReleased() {
		//NO OP//
	}
}
