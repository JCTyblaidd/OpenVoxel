package net.openvoxel.client.gui_framework;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.GUIRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 08/04/2017.
 *
 * Area with a scroll bar
 */
public class GUIVScrollArea extends GUIObjectSizable {

	private List<GUIObject> subObjects;
	private float absOffset = 0;
	private float maxOffset = 0;

	public GUIVScrollArea() {
		subObjects = new ArrayList<>();
	}

	public void add(GUIObject object) {
		subObjects.add(object);
	}

	@Override
	public void Draw(GUIRenderer.GUITessellator drawHandle) {
		DrawSquare(drawHandle,null,0xFF5500CC);
		final float screenWidth = drawHandle.getScreenWidth();
		final float screenHeight = drawHandle.getScreenHeight();
		final float X1 = getPosX(screenWidth);
		final float Y1 = getPosY(screenHeight);
		final float X2 = X1 + getWidth(screenWidth);
		final float Y2 = Y1 + getHeight(screenHeight);
		drawHandle.Begin();
		drawHandle.EnableTexture(false);
		drawHandle.VertexWithUV(X2,Y2,1,1);
		drawHandle.VertexWithUV(X1,Y2,0,1);
		drawHandle.VertexWithUV(X1,Y1,0,0);
		drawHandle.VertexWithUV(X2,Y1,1,0);
		drawHandle.VertexWithUV(X2,Y2,1,1);
		drawHandle.VertexWithUV(X1,Y1,0,0);
		drawHandle.Draw();
	}

	@Override
	public void OnMouseMove(float newX, float newY, float oldX, float oldY) {
		final float delta = absOffset / ClientInput.currentWindowHeight.get();
		for(GUIObject object : subObjects) {
			object.OnMouseMove(newX,newY - delta,oldX,oldY - delta);
		}
	}

	@Override
	public void OnMousePress(double x, double y) {
		final float delta = absOffset / ClientInput.currentWindowHeight.get();
		for(GUIObject object : subObjects) {
			object.OnMousePress(x,y-delta);
		}
	}

	@Override
	public void OnMouseRelease(double x, double y) {
		final float delta = absOffset / ClientInput.currentWindowHeight.get();
		for(GUIObject object : subObjects) {
			object.OnMouseRelease(x,y-delta);
		}
	}
}
