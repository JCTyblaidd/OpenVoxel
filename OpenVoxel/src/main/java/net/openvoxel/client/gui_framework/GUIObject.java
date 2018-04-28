package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.common.IGuiRenderer;

/**
 * Created by James on 25/08/2016.
 *
 * Important: Calls from the GUI to the Game Thread MUST be synchronised
 */
public abstract class GUIObject {

	public abstract void Draw(IGuiRenderer drawHandle);

	public void OnMousePress(float x, float y, float screenWidth, float screenHeight) {}
	public void OnMouseRelease(float x, float y, float screenWidth, float screenHeight) {}
	public void OnMouseMove(float newX, float newY, float oldX, float oldY, float screenWidth, float screenHeight) {}

	public boolean isDrawDirty() {
		return false;
	}
}
