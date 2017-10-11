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
		//Build the command buffer//
		try(MemoryStack stack = stackPush()) {

			VkCommandBuffer cmdBuffer = new VkCommandBuffer(state.command_buffers_main.get(state.swapChainImageIndex),state.renderDevice.device);
			vkResetCommandBuffer(cmdBuffer,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
			beginInfo.pInheritanceInfo(null);
			vkBeginCommandBuffer(cmdBuffer,beginInfo);

			VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
			renderPassInfo.pNext(VK_NULL_HANDLE);
			renderPassInfo.renderPass(state.renderPass.render_pass);
			renderPassInfo.framebuffer(state.targetFrameBuffers.get(state.swapChainImageIndex));
			VkRect2D screenRect = VkRect2D.callocStack(stack);
			screenRect.extent(state.swapExtent);
			renderPassInfo.renderArea(screenRect);
			VkClearValue.Buffer clearValues = VkClearValue.callocStack(1,stack);
			VkClearColorValue clearColorValue = VkClearColorValue.callocStack(stack);
			clearColorValue.float32(0,0.3f);
			clearColorValue.float32(1,0.0f);
			clearColorValue.float32(2,0.2f);
			clearColorValue.float32(3,1.0f);
			clearValues.color(clearColorValue);
			renderPassInfo.pClearValues(clearValues);

			if(drawCount != 0) {
				ByteBuffer memMapping = mgr.mapMemory(mgr.memGuiStaging.get(1), 0, GUI_BUFFER_SIZE, stack);
				memMapping.put(this.memMap);
				mgr.unMapMemory(mgr.memGuiStaging.get(1));

				VkBufferCopy.Buffer copyInfo = VkBufferCopy.mallocStack(1, stack);
				copyInfo.srcOffset(0);
				copyInfo.dstOffset(0);
				copyInfo.size(drawCount);
				vkCmdCopyBuffer(cmdBuffer, mgr.memGuiStaging.get(0), mgr.memGuiDrawing.get(0), copyInfo);
			}

			vkCmdBeginRenderPass(cmdBuffer,renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

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
			vkCmdEndRenderPass(cmdBuffer);

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
	public void EnableColour(boolean enabled) {
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
