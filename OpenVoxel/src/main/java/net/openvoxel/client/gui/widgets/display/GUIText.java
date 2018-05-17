package net.openvoxel.client.gui.widgets.display;

import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;

/**
 * Created by James on 08/09/2016.
 *
 * IMPORTANT: only height plays are role regarding sizing
 */
public class GUIText extends GUIObjectSizable {

	private String text;
	private float widthLim = 1.0F;

	public GUIText(String str,float widthLim) {
		text = str;
		this.widthLim = widthLim;
	}

	public void updateText(String text) {
		this.text = text;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		DrawSquareWithText(drawHandle,null,0,text,widthLim);
	}

}
