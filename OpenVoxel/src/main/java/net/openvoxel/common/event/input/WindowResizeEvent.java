package net.openvoxel.common.event.input;

import net.openvoxel.common.event.AbstractEvent;

/**
 * Created by James on 01/09/2016.
 */
public class WindowResizeEvent extends AbstractEvent{

	public final int Width;
	public final int Height;

	public WindowResizeEvent(int width,int height) {
		Width = width;
		Height = height;
	}

}
