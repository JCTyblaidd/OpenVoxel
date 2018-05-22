package net.openvoxel.client.gui.game;

import net.openvoxel.client.gui.framework.Screen;
import net.openvoxel.client.renderer.common.IGuiRenderer;

import java.util.ArrayList;
import java.util.List;

public class ScreenCommand extends Screen {

	private boolean commandDirty = false;
	private boolean isTyping = false;

	private List<String> chatHistory = new ArrayList<>();
	private int scrollOffset = 0;

	@Override
	public void DrawScreen(IGuiRenderer tess) {

	}


	private void drawString(IGuiRenderer tess, float x, float y, float h, int idx) {

	}

	//
	// Screen Implementation Details
	//

	public boolean takesOverInput() {
		return isTyping;
	}

	public boolean hidesPreviousScreens() {
		return false;
	}

	public boolean isDrawDirty() {
		boolean dirty = commandDirty;
		commandDirty = false;
		return dirty;
	}
}
