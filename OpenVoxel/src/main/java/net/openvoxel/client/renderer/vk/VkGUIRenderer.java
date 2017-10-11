package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan GUI Renderer
 */
public class VkGUIRenderer implements GUIRenderer, GUIRenderer.GUITessellator {

	public static final int ENTRY_COUNT = 4096;
	public static final int ENTRY_SIZE = ENTRY_COUNT * 5;
	public static final int STATE_COUNT = 16;

	//vulkan buffer information
	private VkDeviceState state;
	private ByteBuffer guiInformation;
	//Unique Draw States//
	private ByteBuffer matrixStates;
	private ByteBuffer drawFlagStates;
	private ByteBuffer textureStates;

	VkGUIRenderer(VkDeviceState state) {
		this.state = state;

	}

	void cleanup() {

	}

	@Override
	public void DisplayScreen(Screen screen) {
		screen.DrawScreen(this);
	}

	@Override
	public void beginDraw() {
		//NO OP//
	}

	@Override
	public void finishDraw() {
		//Build the command buffer//
	}

	@Override
	public void Begin() {

	}

	@Override
	public void Draw() {

	}

	@Override
	public void SetTexture(ResourceHandle handle) {

	}

	@Override
	public void EnableTexture(boolean enabled) {

	}

	@Override
	public void SetMatrix(Matrix4f mat) {

	}

	@Override
	public void EnableColour(boolean enabled) {

	}


	@Override
	public void Vertex(float x, float y) {

	}

	@Override
	public void VertexWithUV(float x, float y, float u, float v) {

	}


	@Override
	public void VertexWithCol(float x, float y, int RGB) {

	}


	@Override
	public void VertexWithColUV(float x, float y, float u, float v, int RGB) {

	}


	@Override
	public void DrawText(float x, float y, float height, String text) {

	}

	@Override
	public void DrawText(float x, float y, float height, String text, int col, int colOutline) {

	}

	@Override
	public void DrawItem(float x, float y, float width, float height) {

	}

	@Override
	public float GetTextWidthRatio(String text) {
		return 0;
	}

	@Override
	public float getScreenWidth() {
		return ClientInput.currentWindowWidth.get();
	}

	@Override
	public float getScreenHeight() {
		return ClientInput.currentWindowHeight.get();
	}

	@Override
	public void resetScissor() {

	}

	@Override
	public void scissor(int x, int y, int w, int h) {

	}
}
