package net.openvoxel.client.gui.inventory;

import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 08/09/2016.
 */
public class GUISlot extends GUIObjectSizable {

	private static ResourceHandle slotImg;
	static {
		//TODO: load
	}
	private GUIInventory inv;
	private int slotID;
	private ResourceHandle resourceHandle;
	private boolean mouseOver;

	public GUISlot(GUIInventory inv,int ID) {
		this(slotImg,inv,ID);
	}
	public GUISlot(ResourceHandle handle,GUIInventory inv,int ID) {
		this.inv = inv;
		this.slotID = ID;
	}

	@Override
	public void onMouseClicked() {
		inv.notifySlotClick(slotID);
	}

	@Override
	public void onMouseReleased() {
		inv.notifySlotRelease(slotID);
	}

	@Override
	public void onMouseEnter() {
		mouseOver = true;
	}

	@Override
	public void onMouseLeave() {
		mouseOver = false;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		if(mouseOver) {
			DrawSquare(drawHandle,resourceHandle,0xFFFFFFFF);
		}else{
			DrawSquare(drawHandle,resourceHandle,0xBBBBBBFF);
		}
	}
}
