package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.IGuiRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by James on 08/04/2017.
 *
 * Area with a scroll bar
 */
public class GUIVScrollArea extends GUIObjectSizable {

	private List<GUIObject> subObjects;
	private float absOffset = 0;
	private float maxOffset = 0;
	private AtomicBoolean isDirty = new AtomicBoolean(false);

	public GUIVScrollArea() {
		subObjects = new ArrayList<>();
	}

	public void add(GUIObject object) {
		subObjects.add(object);
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		DrawSquare(drawHandle,null,0xFF5500CC);
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		final int col = 0xFF446643;
		drawHandle.Begin();
		drawHandle.EnableTexture(false);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y2,0,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.VertexWithColUV(X2,Y1,1,0,col);
		drawHandle.VertexWithColUV(X2,Y2,1,1,col);
		drawHandle.VertexWithColUV(X1,Y1,0,0,col);
		drawHandle.Draw();
		ResizedGUIHandleWrapper resizedTess = new ResizedGUIHandleWrapper(drawHandle);
		resizedTess.set(X1,Y1,X2-X1,Y2-Y1);
		drawHandle.pushScissor(
				(int)(X1*screenWidth),
				(int)(Y1*screenHeight),
				(int)((X2-X1)*screenWidth),
				(int)((Y2-Y1)*screenHeight)
		);
		for(GUIObject sub_object : subObjects) {
			sub_object.Draw(resizedTess);
		}
		drawHandle.popScissor();
	}

	@Override
	public void OnMouseMove(float newX, float newY, float oldX, float oldY,float screenWidth,float screenHeight) {
		final float X = getPosX(screenWidth);
		final float Y = getPosY(screenHeight);
		final float W = getWidth(screenWidth);
		final float H = getHeight(screenHeight);
		final float internal_xNew = (newX - X) / W;
		final float internal_yNew = (newY - Y - absOffset) / H;
		final float internal_xOld = (oldX - X) / W;
		final float internal_yOld = (oldY - Y - absOffset) / H;
		for(GUIObject object : subObjects) {
			object.OnMouseMove(internal_xNew,internal_yNew,internal_xOld,internal_yOld,screenWidth*W,screenHeight*H);
		}
	}

	@Override
	public void OnMousePress(float x, float y, float screenWidth, float screenHeight) {
		final float X = getPosX(screenWidth);
		final float Y = getPosY(screenHeight);
		final float W = getWidth(screenWidth);
		final float H = getHeight(screenHeight);
		final float internal_x = (x - X) / W;
		final float internal_y = (y - Y - absOffset) / H;
		for(GUIObject object : subObjects) {
			object.OnMousePress(internal_x,internal_y,screenWidth*W,screenHeight*H);
		}
	}

	@Override
	public void OnMouseRelease(float x, float y, float screenWidth, float screenHeight) {
		final float X = getPosX(screenWidth);
		final float Y = getPosY(screenHeight);
		final float W = getWidth(screenWidth);
		final float H = getHeight(screenHeight);
		final float internal_x = (x - X) / W;
		final float internal_y = (y - Y - absOffset) / H;
		for(GUIObject object : subObjects) {
			object.OnMouseRelease(internal_x,internal_y,screenWidth*W,screenHeight*H);
		}
	}

	@Override
	public boolean isDrawDirty() {
		boolean is_dirty = isDirty.getAndSet(false);
		for(GUIObject object : subObjects) {
			is_dirty |= object.isDrawDirty();
		}
		return is_dirty;
	}
}
