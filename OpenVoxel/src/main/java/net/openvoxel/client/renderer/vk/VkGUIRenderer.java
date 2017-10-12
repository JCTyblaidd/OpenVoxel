package net.openvoxel.client.renderer.vk;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.client.renderer.vk.util.VkMemoryManager;
import net.openvoxel.common.resources.ResourceHandle;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

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
	private static final int GUI_STATE_CHANGE_LIMIT = 512;
	public static final int GUI_BUFFER_SIZE = GUI_ELEMENT_SIZE * GUI_TRIANGLE_COUNT;

	private VkDeviceState state;
	private VkMemoryManager mgr;
	private int screenWidth;
	private int screenHeight;
	private int drawCount;
	private ByteBuffer writeTarget;
	private Set<ResourceHandle> requestedImages;
	private Matrix4f matrixStackHead = new Matrix4f();
	private List<ResourceHandle> requestedImageStack;
	private FloatBuffer matrixArrayStack;
	private int stateChangeCount, lastStateChange;
	private IntBuffer offsetTransitionStack;
	private ResourceHandle currentHandle;
	private TByteList imageEnableStateStack;

	VkGUIRenderer(VkDeviceState state) {
		this.state = state;
		this.mgr = state.memoryMgr;
		writeTarget = MemoryUtil.memAlloc(GUI_BUFFER_SIZE);
		requestedImages = new HashSet<>();
		requestedImageStack = new ArrayList<>();
		matrixArrayStack = MemoryUtil.memAllocFloat(16 * GUI_STATE_CHANGE_LIMIT);
		offsetTransitionStack = MemoryUtil.memAllocInt(GUI_STATE_CHANGE_LIMIT);
		currentHandle = null;
		imageEnableStateStack = new TByteArrayList(GUI_STATE_CHANGE_LIMIT);
	}

	void cleanup() {
		MemoryUtil.memFree(writeTarget);
		MemoryUtil.memFree(matrixArrayStack);
		MemoryUtil.memFree(offsetTransitionStack);
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
		requestedImages.clear();

		imageEnableStateStack.clear();
		matrixArrayStack.position(0);
		offsetTransitionStack.position(0);
		requestedImageStack.clear();

		imageEnableStateStack.add((byte)0);
		requestedImageStack.add(null);
		matrixStackHead.set(identity_matrix);
		offsetTransitionStack.put(0);
		currentHandle = null;

		stateChangeCount = 0;
		lastStateChange = 0;
	}

	private void startSync(MemoryStack stack) {

	}

	@Override
	public void finishDraw() {
		try(MemoryStack stack = stackPush()) {
			startSync(stack);
			//VkBufferMemoryBarrier.Buffer memoryBarrier = VkBufferMemoryBarrier.mallocStack(1,stack);
			//memoryBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
			//memoryBarrier.pNext(VK_NULL_HANDLE);
			//memoryBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			//memoryBarrier.dstAccessMask(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT);
			//memoryBarrier.srcQueueFamilyIndex(state.renderDevice.queueFamilyIndexTransfer);
			//memoryBarrier.dstQueueFamilyIndex(state.renderDevice.queueFamilyIndexRender);
			//memoryBarrier.offset(0);
			//memoryBarrier.size(drawCount);
			//memoryBarrier.buffer(mgr.memGuiStaging.get(0));

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
				writeTarget.position(0);
				memMapping.put(writeTarget);
				memMapping.position(0);
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

			//vkCmdPipelineBarrier(cmdBuffer,VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,0
			//		,null,memoryBarrier,null);

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

	private final Matrix4f identity_matrix = new Matrix4f().identity();
	@Override
	public void Begin() {
		SetTexture(null);
		EnableTexture(false);
		SetMatrix(identity_matrix);
	}

	@Override
	public void Draw() {
		//NO OP//
	}

	private boolean has_drawn_since_state_change() {
		return lastStateChange != drawCount;
	}

	private void state_change() {
		offsetTransitionStack.put(drawCount / GUI_ELEMENT_SIZE);
		lastStateChange = drawCount;
	}

	@Override
	public void SetTexture(ResourceHandle handle) {
		System.out.println("SET:TEX -- " + handle);
		if(!requestedImages.contains(handle)) {
			requestedImages.add(handle);
		}
		ResourceHandle latest = requestedImageStack.get(stateChangeCount);
		if(handle != null && !handle.equals(latest)) {
			if(has_drawn_since_state_change() && latest != null) {
				requestedImageStack.add(handle);
				imageEnableStateStack.add(imageEnableStateStack.get(stateChangeCount));
				int start = stateChangeCount * 16;
				for(int i = 0; i < 16; i++) {
					matrixArrayStack.put(start+16+i,matrixArrayStack.get(start+i));
				}
				state_change();
			}else{
				requestedImageStack.set(stateChangeCount,handle);
			}
		}
	}

	@Override
	public void EnableTexture(boolean enabled) {
		System.out.println("Enable:TEX -- " + enabled);
		byte change = enabled ? (byte)1 : (byte)0;
		if(imageEnableStateStack.get(stateChangeCount) != change) {
			if(has_drawn_since_state_change()) {
				requestedImageStack.add(requestedImageStack.get(stateChangeCount));
				imageEnableStateStack.add(change);
				int start = stateChangeCount * 16;
				for(int i = 0; i < 16; i++) {
					matrixArrayStack.put(start+16+i,matrixArrayStack.get(start+i));
				}
				state_change();
			}else{
				imageEnableStateStack.set(stateChangeCount,change);
			}
		}
	}

	@Override
	public void SetMatrix(@NotNull Matrix4f mat) {
		System.out.println("Set:Matrix -- identity="+mat.equals(identity_matrix));
		if(!matrixStackHead.equals(mat)) {
			if(has_drawn_since_state_change()) {
				requestedImageStack.add(requestedImageStack.get(stateChangeCount));
				imageEnableStateStack.add(imageEnableStateStack.get(stateChangeCount));
				int pos = matrixArrayStack.position();
				mat.set(matrixArrayStack);
				matrixArrayStack.position(pos + 16);
				state_change();
			}else{
				int pos = matrixArrayStack.position();
				matrixArrayStack.position(pos - 16);
				mat.set(matrixArrayStack);
				matrixArrayStack.position(pos);
			}
		}
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
		writeTarget.putFloat(drawCount,x*2-1);
		writeTarget.putFloat(drawCount+4,y*2-1);
		writeTarget.putFloat(drawCount+8,u);
		writeTarget.putFloat(drawCount+12,v);
		writeTarget.putInt(drawCount+16,RGB);
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
