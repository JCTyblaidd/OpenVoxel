package net.openvoxel.client.gui.widgets;

import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by James on 25/08/2016.
 *
 * Button GUI Handle
 */
public class GUIButton extends GUIObjectSizable {

	protected String str;
	private static ResourceHandle texButton = ResourceManager.getImage("gui/button0");
	private static final float TXT_WIDTH_LIM = 0.90F;
	private boolean inButton = false;
	private boolean drawDirtyFlag = false;

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
	public void Draw(IGuiRenderer drawHandle) {
		if(inButton) {
			DrawSquare(drawHandle, texButton, 0xFFFFDD55);
		}else{
			DrawSquare(drawHandle, texButton);
		}
		if(str != null) {
			float X = getPosX(drawHandle.getScreenWidth());
			float Y = getPosY(drawHandle.getScreenHeight());
			float W = getWidth(drawHandle.getScreenWidth());
			float H = getHeight(drawHandle.getScreenHeight());
			float TXT_RATIO = drawHandle.GetTextWidthRatio(str);
			float TXT_W = TXT_RATIO * H;
			if(TXT_W < W * TXT_WIDTH_LIM) {
				X += (W / 2) - (TXT_W / 2);
				Y += H;
				drawHandle.DrawText(X, Y, H, str);
			}else{
				X += W * (1-TXT_WIDTH_LIM)/2;
				float TXT_H = W / TXT_RATIO * TXT_WIDTH_LIM;
				Y += TXT_H + (H - TXT_H)/2;
				drawHandle.DrawText(X,Y,TXT_H,str);
			}
		}
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
		drawDirtyFlag = true;
	}

	@Override
	public void onMouseLeave() {
		inButton = false;
		drawDirtyFlag = true;
	}

	@Override
	public boolean isDrawDirty() {
		boolean tmp = drawDirtyFlag;
		drawDirtyFlag = false;
		return tmp;
	}
}
