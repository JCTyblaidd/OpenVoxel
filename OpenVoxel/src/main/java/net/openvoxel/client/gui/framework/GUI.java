package net.openvoxel.client.gui.framework;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.debug.Validate;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by James on 25/08/2016.
 *
 * TODO: Remove Synchronisation
 *
 */
public abstract class GUI {

	private static final Deque<Screen> guiStack = new ArrayDeque<>();
	private static final Logger guiLogger = Logger.getLogger("GUI");

	private static boolean usesInput = false;

	static {
		Validate.IsMainThread();
		GUIHandler.Init();
	}

	@PublicAPI
	public static void addScreen(Screen screen) {
		Validate.IsMainThread();
		guiStack.push(screen);
		updateUses();
	}

	@PublicAPI
	public static void removeScreen(Screen screen) {
		Validate.IsMainThread();
		guiStack.remove(screen);
		updateUses();
	}

	@PublicAPI
	public static void removeLastScreen() {
		Validate.IsMainThread();
		guiStack.pop();
		updateUses();
	}

	@PublicAPI
	public static void removeAllScreens() {
		Validate.IsMainThread();
		guiStack.clear();
		usesInput = false;
	}

	@PublicAPI
	public static Deque<Screen> getStack() {
		return guiStack;
	}

	@PublicAPI
	public static boolean isInputTaken() {
		return usesInput;
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

	static void handleMouseMoveEvent(float oldX, float oldY, float newX, float newY,float screenWidth,float screenHeight) {
		try {
			if (!guiStack.isEmpty()) {
				guiStack.getFirst().handleMouseMove(oldX, oldY, newX, newY, screenWidth, screenHeight);
			}
		}catch(Exception ex) {
			guiLogger.StackTrace(ex);
		}
	}
	static void handleMousePress(float X, float Y,float screenWidth, float screenHeight) {
		try {
			if (!guiStack.isEmpty()) {
				guiStack.getFirst().handleMousePress(X, Y, screenWidth, screenHeight);
			}
		}catch(Exception ex) {
			guiLogger.StackTrace(ex);
		}
	}
	static void handleMouseRelease(float X, float Y, float screenWidth, float screenHeight) {
		try {
			if (!guiStack.isEmpty()) {
				guiStack.getFirst().handleMouseRelease(X, Y, screenWidth, screenHeight);
			}
		}catch(Exception ex) {
			guiLogger.StackTrace(ex);
		}
	}
}
