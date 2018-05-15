package net.openvoxel.client;

import net.openvoxel.client.gui.framework.GUI;
import org.joml.Vector2d;
import org.joml.Vector2i;

/**
 * Created by James on 25/08/2016.
 *
 * Main Input Handle
 */
public class ClientInput {

	public static Vector2d unhandledMouseDelta;
	public static Vector2d mousePosition;
	public static Vector2i currentWindowLocation;
	public static Vector2i currentWindowFrameSize;

	public static boolean[] currentInputStatus;

	public static boolean inputTakenByGUI() {
		return GUI.isInputTaken();
	}

	static {
		unhandledMouseDelta = new Vector2d(0,0);
		mousePosition = new Vector2d(0,0);
		currentWindowLocation = new Vector2i(0,0);
		currentWindowFrameSize = new Vector2i(1920,1080);
		currentInputStatus = new boolean[512];
		for(int i = 0; i < 512; i++) {
			currentInputStatus[i] = false;
		}
	}

	/***
	 *
	 * @param glfw_key a key, or mouse identifier
	 * @return if it is enabled
	 */
	public static boolean isKeyDown(int glfw_key) {
		return currentInputStatus[glfw_key];
	}

	public static void resetMouseDelta() {
		unhandledMouseDelta.x = 0;
		unhandledMouseDelta.y = 0;
	}

}
