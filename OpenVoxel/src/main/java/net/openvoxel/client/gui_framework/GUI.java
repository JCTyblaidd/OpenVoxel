package net.openvoxel.client.gui_framework;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by James on 25/08/2016.
 *
 * TODO: Remove Synchronisation
 *
 */
public abstract class GUI {

	private static Deque<Screen> guiStack = new ArrayDeque<>();
	private static boolean usesInput = false;
	static {
		GUIHandler.Init();
	}

	private static void updateUses() {
		for(Screen screen : guiStack) {
			if(screen.takesOverInput()) {
				usesInput = true;
				return;
			}
		}
		usesInput = false;
	}

	private static Screen asyncUpdate = null;
	public static void addAsyncScreen(Screen screen) {
		asyncUpdate = screen;
	}

	public static synchronized void addScreen(Screen screen) {
		guiStack.push(screen);
		updateUses();
	}
	public static synchronized void removeScreen(Screen screen) {
		guiStack.remove(screen);
		updateUses();
	}
	public static synchronized void removeLastScreen() {
		guiStack.pop();
		updateUses();
	}
	public static synchronized void removeAllScreens() {
		guiStack.clear();
		usesInput = false;
	}

	static synchronized void handleMouseMoveEvent(float oldX, float oldY, float newX, float newY,float screenWidth,float screenHeight) {
		if(!guiStack.isEmpty()) {
			guiStack.getFirst().handleMouseMove(oldX, oldY, newX, newY,screenWidth,screenHeight);
		}
	}
	static synchronized void handleMousePress(float X, float Y,float screenWidth, float screenHeight) {
		if(!guiStack.isEmpty()) {
			guiStack.getFirst().handleMousePress(X, Y,screenWidth,screenHeight);
		}
	}
	static synchronized void handleMouseRelease(float X, float Y, float screenWidth, float screenHeight) {
		if(!guiStack.isEmpty()) {
			guiStack.getFirst().handleMouseRelease(X, Y,screenWidth,screenHeight);
		}
	}

	public static Deque<Screen> getStack() {
		if(asyncUpdate != null) {
			guiStack.push(asyncUpdate);
			asyncUpdate = null;
		}
		return guiStack;
	}

	public static boolean isInputTaken() {
		return usesInput;
	}
}
