package net.openvoxel.client.gui.widgets.input;

import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;
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
			DrawSquareWithText(drawHandle,texButton,0xFFFFDD55,str,TXT_WIDTH_LIM);
		}else{
			DrawSquareWithText(drawHandle,texButton,0xFFFFFFFF,str,TXT_WIDTH_LIM);
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
