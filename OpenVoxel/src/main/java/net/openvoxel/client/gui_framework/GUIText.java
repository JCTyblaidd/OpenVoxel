package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 08/09/2016.
 *
 * IMPORTANT: only height plays are role regarding sizing
 */
public class GUIText extends GUIObjectSizable{

	private String text;

	public GUIText(String str) {
		text = str;
	}

	public synchronized void updateText(String text) {
		this.text = text;
	}

	@Override
	public synchronized void Draw(GUIRenderer.GUITessellator drawHandle) {
		float X = getPosX(drawHandle.getScreenWidth());
		float Y = getPosY(drawHandle.getScreenHeight());
		float H = getHeight(drawHandle.getScreenHeight());
		float diff = drawHandle.GetTextWidthRatio(text);
		Y -= (H / 2);
		X -= (H * diff / 2);
		drawHandle.DrawText(X,Y,H,text);
	}

}
