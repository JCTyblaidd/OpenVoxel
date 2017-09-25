package net.openvoxel.client.gui_framework;

import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

import java.util.function.Consumer;

/**
 * Created by James on 25/08/2016.
 *
 * Button GUI Handle
 */
public class GUIButton extends GUIObjectSizable {

	protected String str;
	private static ResourceHandle texButton = ResourceManager.getImage("gui/button0");
	private boolean inButton = false;

	private Consumer<GUIButton> onButtonPressFunc = null;

	public GUIButton(String buttonName) {
		str = buttonName;
	}

	public void setAction(Consumer<GUIButton> handler) {
		onButtonPressFunc = handler;
	}

	public void setAction(Runnable handler) {
		onButtonPressFunc = (button) -> handler.run();
	}

	@Override
	public void Draw(GUIRenderer.GUITessellator drawHandle) {
		if(inButton) {
			DrawSquare(drawHandle, texButton, 0xFFFFDD55);
		}else{
			DrawSquare(drawHandle, texButton);
		}
		if(str != null) {
			float X = getPosX(drawHandle.getScreenWidth());
			float Y = getPosY(drawHandle.getScreenHeight());
			float H = getHeight(drawHandle.getScreenHeight()) * 2;
			float W = getWidth(drawHandle.getScreenWidth());
			float TXT_W = drawHandle.GetTextWidthRatio(str) * H;
			float W_OFF = W - (TXT_W/2);
			X = X * 2 - 1 + W_OFF;
			Y = -Y * 2 + 1 - H;
			drawHandle.DrawText(X,Y,H,str);
		}
		//drawHandle.DrawText(0.1F,0.1F,0.1F,"Open Voxel Version 300000!!!! #Awesome");
	}


	@Override
	public void onMouseClicked() {
		if(onButtonPressFunc != null) {
			onButtonPressFunc.accept(this);
		}
	}

	@Override
	public void onMouseEnter() {
		inButton = true;
	}

	@Override
	public void onMouseLeave() {
		inButton = false;
	}
}
