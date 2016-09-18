package net.openvoxel.client.gui;

import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 25/08/2016.
 */
public abstract class ConfigureableValue<T> {

	private T t;

	public T getT() {
		return t;
	}
	public void setT(T t2) {
		t = t2;
	}
	public void drawEntry(GUIRenderer.GUITessellator tessellator, float xpos, float ypos) {

	}

}
