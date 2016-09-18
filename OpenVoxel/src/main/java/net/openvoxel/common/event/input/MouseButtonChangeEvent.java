package net.openvoxel.common.event.input;

import net.openvoxel.common.event.AbstractEvent;

/**
 * Created by James on 01/09/2016.
 */
public class MouseButtonChangeEvent extends AbstractEvent{

	public final int GLFW_BUTTON;
	public final boolean PRESSED;

	public MouseButtonChangeEvent(int button,boolean pressed) {
		GLFW_BUTTON = button;
		PRESSED = pressed;
	}

}
