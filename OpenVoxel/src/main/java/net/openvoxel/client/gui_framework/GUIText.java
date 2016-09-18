package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.generic.GUIRenderer;

/**
 * Created by James on 08/09/2016.
 *
 * IMPORTANT: only height plays are role regarding sizing
 */
public class GUIText extends GUIObjectSizable{

	private final String text;

	public GUIText(String str) {
		text = str;
	}

	@Override
	public void Draw(GUIRenderer.GUITessellator drawHandle) {
		float X = getPosX(drawHandle.getScreenWidth());
		float Y = getPosY(drawHandle.getScreenHeight());
		float H = getHeight(drawHandle.getScreenHeight());
		float diff = drawHandle.GetTextWidthRatio(text);
		X = X * 2 - 1 - (H * diff / 2);
		Y = -Y * 2 + 1 + (H / 2);
		drawHandle.DrawText(X,Y,H,text);
	}

}
