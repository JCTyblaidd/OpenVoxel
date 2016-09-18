package net.openvoxel.client.gui.util;

import net.openvoxel.client.gui_framework.GUIObjectImage;
import net.openvoxel.client.gui_framework.GUIText;

/**
 * Created by James on 11/09/2016.
 */
public class ScreenLoading {

	private float progress;
	private GUIObjectImage background;
	private GUIText title;

	public ScreenLoading(String name) {
		progress = 0;

	}

	public void updateProgress(float val) {
		progress = val;
	}

	public void incProgress(float increase) {
		updateProgress(increase+getProgress());
	}

	public float getProgress() {
		return progress;
	}

}
