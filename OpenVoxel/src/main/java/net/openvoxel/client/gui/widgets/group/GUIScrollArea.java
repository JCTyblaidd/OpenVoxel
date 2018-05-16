package net.openvoxel.client.gui.widgets.group;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.gui.framework.GUIObject;
import net.openvoxel.client.gui.framework.ResizedGuiRenderer;
import net.openvoxel.client.gui.widgets.GUIObjectSizable;
import net.openvoxel.client.renderer.common.IGuiRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 14/09/2016.
 */
public class GUIScrollArea extends GUIObjectSizable {

	public final List<GUIObject> guiObjects;
	public float scrollPercent;

	public GUIScrollArea() {
		guiObjects = new ArrayList<>();
		scrollPercent = 0;
	}

	@Override
	public void Draw(IGuiRenderer drawHandle) {
		final float xpos = getPosX(drawHandle.getScreenWidth());
		final float ypos = getPosY(drawHandle.getScreenHeight());
		final float height = getHeight(drawHandle.getScreenHeight());
		final float width = getWidth(drawHandle.getScreenWidth());
		drawHandle.pushScissor( (int)(xpos * ClientInput.currentWindowFrameSize.x),
							(int)(ypos * ClientInput.currentWindowFrameSize.y),
							(int)(width * ClientInput.currentWindowFrameSize.x),
							(int)(height * ClientInput.currentWindowFrameSize.y));
		ResizedGuiRenderer resizeHandle = new ResizedGuiRenderer(drawHandle);
		resizeHandle.set(xpos,ypos,width,1);
		for(GUIObject object : guiObjects) {
			object.Draw(resizeHandle);
		}
		drawHandle.popScissor();
	}
}