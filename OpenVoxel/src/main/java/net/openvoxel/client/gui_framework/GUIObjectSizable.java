package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 01/09/2016.
 */
public abstract class GUIObjectSizable extends GUIObject{

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
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin();
		drawHandle.EnableTexture(Image != null);
		if(Image != null) {
			drawHandle.SetTexture(Image);
		}
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y2,0,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.VertexWithColUV(X2,Y1,1,0,col);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.Draw();
	}


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
