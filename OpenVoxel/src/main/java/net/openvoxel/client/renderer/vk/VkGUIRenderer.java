package net.openvoxel.client.renderer.vk;

import com.jc.util.reflection.ReflectedUnsafe;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.control.Renderer;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.renderer.vk.util.VkMemoryManager;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Random;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan GUI Renderer
 */
public class VkGUIRenderer implements GUIRenderer, GUIRenderer.GUITessellator {

	//vulkan buffer information
	private static final int GUI_ELEMENT_SIZE = (4 + 4 + 4 + 4 + 4);
	private static final int GUI_TRIANGLE_COUNT = 4096;
	public static final int GUI_BUFFER_SIZE = GUI_ELEMENT_SIZE * GUI_TRIANGLE_COUNT;

	private VkDeviceState state;
	private VkMemoryManager mgr;
	private int screenWidth;
	private int screenHeight;
	private int drawCount;
	private ByteBuffer memMap;

	private Random debug = new Random();
	VkGUIRenderer(VkDeviceState state) {
		this.state = state;
		this.mgr = state.memoryMgr;
		memMap = MemoryUtil.memAlloc(GUI_BUFFER_SIZE);
	}

	void cleanup() {
		MemoryUtil.memFree(memMap);
	}

	@Override
	public void DisplayScreen(Screen screen) {
		screen.DrawScreen(this);
	}

	@Override
	public void beginDraw() {
		screenWidth = ClientInput.currentWindowWidth.get();
		screenHeight = ClientInput.currentWindowHeight.get();
		drawCount = 0;
	}

	@Override
	public void finishDraw() {
		try(MemoryStack stack = stackPush()) {

			VkCommandBuffer transferBuffer = new VkCommandBuffer(state.command_buffers_gui_transfer.get(state.swapChainImageIndex),state.renderDevice.device);
			vkResetCommandBuffer(transferBuffer,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(null);
			vkBeginCommandBuffer(transferBuffer,beginInfo);

			if(drawCount != 0) {
				ByteBuffer memMapping = mgr.mapMemory(mgr.memGuiStaging.get(1), 0, GUI_BUFFER_SIZE, stack);
				memMapping.position(0);
				memMapping.put(this.memMap);
				mgr.unMapMemory(mgr.memGuiStaging.get(1));

				VkBufferCopy.Buffer copyInfo = VkBufferCopy.mallocStack(1, stack);
				copyInfo.srcOffset(0);
				copyInfo.dstOffset(0);
				copyInfo.size(drawCount);
				vkCmdCopyBuffer(transferBuffer, mgr.memGuiStaging.get(0), mgr.memGuiDrawing.get(0), copyInfo);
			}

			if(vkEndCommandBuffer(transferBuffer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to record transfer buffer");
			}

			VkCommandBuffer cmdBuffer = new VkCommandBuffer(state.command_buffers_gui.get(state.swapChainImageIndex),state.renderDevice.device);
			vkResetCommandBuffer(cmdBuffer,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
			VkCommandBufferInheritanceInfo inheritance = VkCommandBufferInheritanceInfo.mallocStack(stack);
			inheritance.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO);
			inheritance.pNext(VK_NULL_HANDLE);
			inheritance.renderPass(state.renderPass.render_pass);
			inheritance.subpass(0);
			inheritance.framebuffer(state.targetFrameBuffers.get(state.swapChainImageIndex));
			inheritance.occlusionQueryEnable(false);
			inheritance.queryFlags(0);
			inheritance.pipelineStatistics(0);
			beginInfo.pInheritanceInfo(inheritance);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT | VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
			vkBeginCommandBuffer(cmdBuffer,beginInfo);

			vkCmdBindPipeline(cmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, state.guiPipeline.graphics_pipeline);

			VkRect2D.Buffer scissor = VkRect2D.mallocStack(1,stack);
			VkOffset2D offset = VkOffset2D.callocStack(stack);
			scissor.offset(offset);
			VkExtent2D extent = VkExtent2D.mallocStack(stack);
			extent.set(screenWidth,screenHeight);
			scissor.extent(extent);

			vkCmdSetScissor(cmdBuffer,0,scissor);

			if(drawCount != 0) {
				vkCmdBindVertexBuffers(cmdBuffer, 0, stack.longs(mgr.memGuiDrawing.get(0)), stack.longs(0));

				vkCmdDraw(cmdBuffer, drawCount / GUI_ELEMENT_SIZE, 1, 0, 0);
			}

			if(vkEndCommandBuffer(cmdBuffer) != VK_SUCCESS) {
				throw new RuntimeException("Failed to draw command buffer");
			}
		}
	}

	@Override
	public void Begin() {
		//TODO
	}

	@Override
	public void Draw() {
		//TODO
	}

	@Override
	public void SetTexture(ResourceHandle handle) {
		//TODO
	}

	@Override
	public void EnableTexture(boolean enabled) {
		//TODO
	}

	@Override
	public void SetMatrix(Matrix4f mat) {
		//TODO
	}

	@Override
	public void Vertex(float x, float y) {
		VertexWithColUV(x,y,0,0,0xFFFFFFFF);
	}

	@Override
	public void VertexWithUV(float x, float y, float u, float v) {
		VertexWithColUV(x,y,u,v,0xFFFFFFFF);
	}


	@Override
	public void VertexWithCol(float x, float y, int RGB) {
		VertexWithColUV(x,y,0,0,RGB);
	}


	@Override
	public void VertexWithColUV(float x, float y, float u, float v, int RGB) {
		float xTrans = debug.nextFloat() * 0.01F;
		x = x + xTrans;
		float yTrans = debug.nextFloat() * 0.01F;
		y = y + yTrans;
		memMap.putFloat(drawCount,x*2-1);
		memMap.putFloat(drawCount+4,y*2-1);
		memMap.putFloat(drawCount+8,u);
		memMap.putFloat(drawCount+12,v);
		memMap.putInt(drawCount+16,RGB);
		drawCount += GUI_ELEMENT_SIZE;
	}


	@Override
	public void DrawText(float x, float y, float height, String text) {
		//TODO:
	}

	@Override
	public void DrawText(float x, float y, float height, String text, int col, int colOutline) {
		//TODO:
	}

	@Override
	public void DrawItem(float x, float y, float width, float height) {
		//TODO:
	}

	@Override
	public float GetTextWidthRatio(String text) {
		return 0;
	}

	@Override
	public float getScreenWidth() {
		return screenWidth;
	}

	@Override
	public float getScreenHeight() {
		return screenHeight;
	}

	@Override
	public void resetScissor() {
		//TODO:
	}

	@Override
	public void scissor(int x, int y, int w, int h) {
		//TODO:
	}
}
