package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.generic.GUIRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 25/08/2016.
 *
 * Standard Screen Display Interface
 */
public class Screen {

	public List<GUIObject> guiObjects;

	public Screen() {
		guiObjects = new ArrayList<>();
	}


	public void DrawScreen(GUIRenderer.GUITessellator tess) {
		for(GUIObject object : guiObjects) {
			object.Draw(tess);
		}
	}

	public void handleMouseMove(float oldX, float oldY, float newX, float newY) {
		guiObjects.forEach(o -> o.OnMouseMove(newX,newY,oldX,oldY));
	}
	public void handleMousePress(float X, float Y) {
		guiObjects.forEach(o -> o.OnMousePress(X,Y));
	}
	public void handleMouseRelease(float X, float Y) {
		guiObjects.forEach(o -> o.OnMouseRelease(X,Y));
	}

	public boolean takesOverInput() {
		return true;
	}

}
