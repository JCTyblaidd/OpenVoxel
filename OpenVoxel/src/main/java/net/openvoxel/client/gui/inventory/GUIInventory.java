package net.openvoxel.client.gui.inventory;

import net.openvoxel.client.STBITexture;
import net.openvoxel.client.gui_framework.GUIObjectImage;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 08/09/2016.
 */
public abstract class GUIInventory extends Screen{

	protected final int img_w;
	protected final int img_h;

	public GUIInventory(ResourceHandle image) {
		GUIObjectImage img = new GUIObjectImage(image);
		STBITexture tex = new STBITexture(image.getByteData());
		img_w = tex.getWidth();
		img_h = tex.getHeight();
		tex.Free();
		img.setCentered(img_w,img_h);
		guiObjects.add(img);
	}

	protected void addSlot(int X, int Y,int slotID) {
		GUISlot slot = new GUISlot(this,slotID);


		guiObjects.add(slot);
	}

	public void notifySlotClick(int slotID) {
		//TODO:
	}

	public void notifySlotRelease(int slotID) {

	}
}
