package net.openvoxel.client.gui_framework;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.generic.GUIRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 14/09/2016.
 */
public class GUIScrollArea extends GUIObjectSizable{

	public final List<GUIObject> guiObjects;
	public float scrollPercent;

	public GUIScrollArea() {
		guiObjects = new ArrayList<>();
		scrollPercent = 0;
	}

	@Override
	public void Draw(GUIRenderer.GUITessellator drawHandle) {
		final float xpos = getPosX(drawHandle.getScreenWidth());
		final float ypos = getPosY(drawHandle.getScreenHeight());
		final float height = getHeight(drawHandle.getScreenHeight());
		final float width = getWidth(drawHandle.getScreenWidth());
		drawHandle.scissor( (int)(xpos * ClientInput.currentWindowWidth),
							(int)(ypos * ClientInput.currentWindowHeight),
							(int)(width * ClientInput.currentWindowWidth),
							(int)(height * ClientInput.currentWindowHeight));
		ResizedGUIHandleWrapper resizeHandle = new ResizedGUIHandleWrapper(drawHandle);
		resizeHandle.set(xpos,ypos,width,1);
		for(GUIObject object : guiObjects) {
			object.Draw(resizeHandle);
		}
		drawHandle.resetScissor();
	}
}
