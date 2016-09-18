package net.openvoxel.common.event.input;

import net.openvoxel.common.event.AbstractEvent;

/**
 * Created by James on 01/09/2016.
 */
public class CursorPositionChangeEvent extends AbstractEvent{

	public final float X;
	public final float Y;

	public CursorPositionChangeEvent(float X, float Y) {
		this.X = X;
		this.Y = Y;
	}

}
