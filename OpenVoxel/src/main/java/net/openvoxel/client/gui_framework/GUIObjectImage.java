package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

/**
 * Created by James on 01/09/2016.
 */
public class GUIObjectImage extends GUIObjectSizable{

	private ResourceHandle handle;

	public GUIObjectImage(String imageID) {
		handle = ResourceManager.getImage(imageID);
	}

	public GUIObjectImage(ResourceHandle handle) {
		this.handle = handle;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		DrawSquare(drawHandle,handle);
	}
}
