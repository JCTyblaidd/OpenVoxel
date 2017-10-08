package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.gui_framework.Screen;
import net.openvoxel.client.renderer.generic.GUIRenderer;
import net.openvoxel.client.renderer.vk.util.VkDeviceState;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Created by James on 17/04/2017.
 *
 * Vulkan GUI Renderer
 */
public class VKGUIRenderer implements GUIRenderer, GUIRenderer.GUITessellator {

	//vulkan buffer information
	private long gui_render_buffer = 0;
	private long gui_render_memory = 0;
	private VkDeviceState state;
	private static final long GUI_BUFFER_SIZE = 65536;
	private long draw_gui_cmd_pool;
	private VkCommandBuffer draw_gui_cmd_buffer;

	VKGUIRenderer(VkDeviceState state) {
		this.state = state;
		try(MemoryStack stack = stackPush()) {
			VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.callocStack(stack);
			bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
			bufferCreateInfo.pNext(VK_NULL_HANDLE);
			bufferCreateInfo.size(GUI_BUFFER_SIZE);
			bufferCreateInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
			bufferCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			LongBuffer longBuffer = stack.mallocLong(1);
			int res = vkCreateBuffer(state.renderDevice.device,bufferCreateInfo,null,longBuffer);
			if(res != VK_SUCCESS) {
				state.vkLogger.Severe("Failed to create GUI Buffer");
				throw new RuntimeException("gui_buffer");
			}
			gui_render_buffer = longBuffer.get(0);
			VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
			vkGetBufferMemoryRequirements(state.renderDevice.device,gui_render_buffer,requirements);
			gui_render_memory = state.renderDevice.allocMemory(stack,requirements,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
			if(gui_render_memory != 0) {
				vkBindBufferMemory(state.renderDevice.device,gui_render_buffer,gui_render_memory,0);
			}else{
				state.vkLogger.Severe("Failed to allocate memory");
				throw new RuntimeException("gui_buffer2");
			}
			init_cmd_buffer(stack);
		}
	}

	void cleanup() {
		vkDestroyBuffer(state.renderDevice.device,gui_render_buffer,null);
		vkFreeMemory(state.renderDevice.device,gui_render_memory,null);
		vkDestroyCommandPool(state.renderDevice.device,draw_gui_cmd_pool,null);
	}

	private void init_cmd_buffer(MemoryStack stack) {
		VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.mallocStack(stack);
		createInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
		createInfo.pNext(VK_NULL_HANDLE);
		createInfo.flags(0);
		createInfo.queueFamilyIndex(state.renderDevice.queueIndexRender);
		LongBuffer lb = stack.callocLong(1);
		int res = vkCreateCommandPool(state.renderDevice.device,createInfo,null,lb);
		if(res != VK_SUCCESS) {
			state.vkLogger.Severe("Failed to create command pool");
			throw new RuntimeException("Failed to create command pool");
		}
		draw_gui_cmd_pool = lb.get(0);

		//Generate Command Buffer//
		VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.mallocStack(stack);
		allocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
		allocateInfo.pNext(VK_NULL_HANDLE);
		allocateInfo.commandPool(draw_gui_cmd_pool);
		allocateInfo.commandBufferCount(1);
		allocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);//TODO: change and update
		PointerBuffer cmd_buffers = stack.mallocPointer(1);
		vkAllocateCommandBuffers(state.renderDevice.device,allocateInfo,cmd_buffers);
		draw_gui_cmd_buffer = new VkCommandBuffer(cmd_buffers.get(0),state.renderDevice.device);

		//Record Said Command Buffer//
		VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
		beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
		beginInfo.pNext(VK_NULL_HANDLE);
		beginInfo.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
		beginInfo.pInheritanceInfo(null);//TODO: change
		vkBeginCommandBuffer(draw_gui_cmd_buffer,beginInfo);

		//Render Code//


		vkEndCommandBuffer(draw_gui_cmd_buffer);
	}

	///
	/// Interface Methods
	///

	@Override
	public void DisplayScreen(Screen screen) {
		screen.DrawScreen(this);
	}

	@Override
	public void beginDraw() {
		//NO OP//
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
	public void SetZ(float zPos) {

	}

	@Override
	public void Vertex(float x, float y) {

	}

	@Override
	public void Vertex(float x, float y, float z) {

	}

	@Override
	public void VertexWithUV(float x, float y, float u, float v) {

	}

	@Override
	public void VertexWithUV(float x, float y, float z, float u, float v) {

	}

	@Override
	public void VertexWithCol(float x, float y, int RGB) {

	}

	@Override
	public void VertexWithCol(float x, float y, float z, int RGB) {

	}

	@Override
	public void VertexWithColUV(float x, float y, float u, float v, int RGB) {

	}

	@Override
	public void VertexWithColUV(float x, float y, float z, float u, float v, int RGB) {

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
