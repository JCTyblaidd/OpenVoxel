package net.openvoxel.common.event.input;

import net.openvoxel.common.event.AbstractEvent;

/**
 * Created by James on 01/09/2016.
 */
public class KeyStateChangeEvent extends AbstractEvent{

	public final int GLFW_KEY;
	public final int GLFW_KEY_STATE;

	public KeyStateChangeEvent(int key, int state) {
		GLFW_KEY = key;
		GLFW_KEY_STATE = state;
	}

}
