package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 25/08/2016.
 *
 * Important: Calls from the GUI to the Game Thread MUST be synchronised
 */
public abstract class GUIObject {

	public abstract void Draw(GUIRenderer.GUITessellator drawHandle);

	public void OnMousePress(double x, double y) {}
	public void OnMouseRelease(double x, double y) {}
	public void OnMouseMove(float newX, float newY, float oldX, float oldY) {}
}
