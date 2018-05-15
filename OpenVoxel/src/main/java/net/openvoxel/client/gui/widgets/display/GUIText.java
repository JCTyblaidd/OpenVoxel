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

	public GUIText(String str) {
		text = str;
	}

	public void updateText(String text) {
		this.text = text;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {

		//float X = getPosX(drawHandle.getScreenWidth());
		//float Y = getPosY(drawHandle.getScreenHeight());
		//float H = getHeight(drawHandle.getScreenHeight());
		//float diff = drawHandle.GetTextWidthRatio(text);
		//Y -= (H / 2);
		//X -= (H * diff / 2);
		//DrawSquare(drawHandle,null,0xFF00FFFF);
		//drawHandle.DrawText(X,Y,H,text);
		DrawSquareWithText(drawHandle,null,0,text,1.0F);
	}

}
