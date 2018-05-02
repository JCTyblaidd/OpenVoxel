package net.openvoxel.client.gui_framework;

import net.openvoxel.OpenVoxel;
import net.openvoxel.client.ClientInput;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.common.event.SubscribeEvents;
import net.openvoxel.common.event.input.CursorPositionChangeEvent;
import net.openvoxel.common.event.input.MouseButtonChangeEvent;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

/**
 * Created by James on 01/09/2016.
 */
public class GUIHandler implements EventListener{

	public static GUIHandler handler;

	public static void Init() {
		handler = new GUIHandler();
	}

	private GUIHandler() {
		OpenVoxel.registerEvents(this);
	}

	@SubscribeEvents
	public void onMouseMove(CursorPositionChangeEvent mouseMove) {
		final float W = ClientInput.currentWindowFrameSize.x;
		final float H = ClientInput.currentWindowFrameSize.y;
		GUI.handleMouseMoveEvent((float)ClientInput.mousePosition.x/W,
				(float)ClientInput.mousePosition.y/H,
				mouseMove.X/W,
				mouseMove.Y/H,
				W,H);
	}

	@SubscribeEvents
	public void onClick(MouseButtonChangeEvent clickEvent) {
		if(clickEvent.GLFW_BUTTON != GLFW_MOUSE_BUTTON_1) return;
		if(clickEvent.PRESSED) {
			GUI.handleMousePress((float)ClientInput.mousePosition.x/ClientInput.currentWindowFrameSize.x,
					(float)ClientInput.mousePosition.y/ClientInput.currentWindowFrameSize.y,
					ClientInput.currentWindowFrameSize.x,ClientInput.currentWindowFrameSize.y);
		}else{
			GUI.handleMouseRelease((float)ClientInput.mousePosition.x/ClientInput.currentWindowFrameSize.x,
					(float)ClientInput.mousePosition.y/ClientInput.currentWindowFrameSize.y,
					ClientInput.currentWindowFrameSize.x,ClientInput.currentWindowFrameSize.y);
		}
	}

}
