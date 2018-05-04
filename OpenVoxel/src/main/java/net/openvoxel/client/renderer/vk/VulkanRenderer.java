package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.renderer.vk.core.VulkanState;
import net.openvoxel.common.event.EventListener;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncRunnablePool;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements EventListener, GraphicsAPI {

	public final VulkanState state;
	private final Logger logger;
	private final VulkanGuiRenderer guiRenderer;
	private final VulkanCache cachedLayout;
	private final VulkanCommandHandler commandHandler;
	private final VulkanTextRenderer textRenderer;
	private final int poolSize;

	//100 millis...
	private static final int TIME_OUT_LENGTH = 100 * 1000 *1000;

	public VulkanRenderer(int asyncCount) {
		poolSize = asyncCount;
		state = new VulkanState();
		logger = Logger.getLogger("Vulkan").getSubLogger("Renderer");
		cachedLayout = new VulkanCache();
		cachedLayout.LoadSingle(state);
		commandHandler = new VulkanCommandHandler(state,cachedLayout);
		commandHandler.init(poolSize);

		//Draw Handlers
		textRenderer = new VulkanTextRenderer("font/font",commandHandler,state.VulkanMemory);
		guiRenderer = new VulkanGuiRenderer(cachedLayout,commandHandler,state.VulkanMemory,textRenderer);
	}

	@Override
	public void close() {
		vkDeviceWaitIdle(state.getLogicalDevice());
		textRenderer.close();
		commandHandler.close();
		cachedLayout.FreeSingle(state.getLogicalDevice());
		guiRenderer.close();
		state.close();
	}

	//////////////////////////////////////
	/// Rendering Handle Functionality ///
	//////////////////////////////////////

	@Override
	public BaseGuiRenderer getGuiRenderer() {
		return guiRenderer;
	}


	///////////////////////////////
	/// Main Loop Functionality ///
	///////////////////////////////


	@Override
	public boolean acquireNextFrame() {
		state.VulkanMemory.updateSystemStatistics();
		boolean success = commandHandler.AcquireNextImage(TIME_OUT_LENGTH);
		if(success) {
			commandHandler.AwaitTransferFence(TIME_OUT_LENGTH);
		}
		return success;
	}

	@Override
	public void prepareForSubmit() {
		commandHandler.AwaitGraphicsFence(TIME_OUT_LENGTH);
	}

	@Override
	public boolean submitNextFrame(AsyncRunnablePool pool, AsyncBarrier barrier) {
		VkCommandBuffer guiTransfer = commandHandler.getGuiDrawCommandBuffer(true);
		VkCommandBuffer guiDrawing = commandHandler.getGuiDrawCommandBuffer(false);

		commandHandler.UpdateTimestamp();

		VkCommandBuffer transferBuffer = commandHandler.getTransferCommandBuffer();
		try(MemoryStack stack = stackPush()) {
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(null);
			vkBeginCommandBuffer(transferBuffer,beginInfo);

			vkEndCommandBuffer(transferBuffer);
			commandHandler.SubmitCommandTransfer(transferBuffer);
		}
		VkCommandBuffer mainBuffer = commandHandler.getMainDrawCommandBuffer();
		try(MemoryStack stack = stackPush()) {
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.mallocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			beginInfo.pNext(VK_NULL_HANDLE);
			beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			beginInfo.pInheritanceInfo(null);
			vkBeginCommandBuffer(mainBuffer,beginInfo);
			commandHandler.CmdResetTimstamps(mainBuffer);
			commandHandler.CmdWriteTimestamp(mainBuffer,0,VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);


			commandHandler.CmdWriteTimestamp(mainBuffer,1,VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
			vkCmdExecuteCommands(mainBuffer,stack.pointers(guiTransfer));

			VkClearValue.Buffer clearValues = VkClearValue.mallocStack(2,stack);
			clearValues.color().float32(0,0.2549f);
			clearValues.color().float32(1,0.72549f);
			clearValues.color().float32(2,1.0f);
			clearValues.color().float32(3,1.0f);
			clearValues.get(1).depthStencil().depth(0.0f);
			VkRenderPassBeginInfo renderPassBegin = VkRenderPassBeginInfo.mallocStack(stack);
			renderPassBegin.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
			renderPassBegin.pNext(VK_NULL_HANDLE);
			renderPassBegin.renderPass(cachedLayout.RENDER_PASS_FORWARD_ONLY.RenderPass);
			renderPassBegin.framebuffer(commandHandler.getFrameBuffer_ForwardOnly());
			renderPassBegin.renderArea().offset().set(0,0);
			renderPassBegin.renderArea().extent(state.chosenSwapExtent);
			renderPassBegin.pClearValues(clearValues);
			vkCmdBeginRenderPass(mainBuffer,renderPassBegin,VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS);

			vkCmdExecuteCommands(mainBuffer,stack.pointers(guiDrawing));

			vkCmdEndRenderPass(mainBuffer);

			commandHandler.CmdWriteTimestamp(mainBuffer,2,VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT);

			vkEndCommandBuffer(mainBuffer);
			commandHandler.SubmitCommandGraphics(mainBuffer);
		}
		boolean success = commandHandler.PresentImage();
		//TODO: REMOVE TEMPORARY HANDLE
		vkDeviceWaitIdle(state.getLogicalDevice());
		return success;
	}

	@Override
	public void startStateChange() {
		vkDeviceWaitIdle(state.getLogicalDevice());
	}

	@Override
	public void stopStateChange() {
		boolean swapSizeChanged = state.recreate();
		boolean renderModeChanged = false;//TODO: IMPLEMENT
		//TODO: CHECK FOR GRAPHICS TYPE REGEN
		if(swapSizeChanged || renderModeChanged) {
			//Total Recreate...
			//TODO: WHERE IS MAIN RENDER MODE STORED??
			commandHandler.close();
			commandHandler.init(poolSize);

			//Reset Swap Partition
			guiRenderer.handleSwapChainSizeChange();
		}else {
			//Window Resize Only...
			commandHandler.reload();
		}
		//TODO: INVALIDATE EVERYTHING
		guiRenderer.invalidateCommands();
	}

	/////////////////////
	/// State Changes ///
	/////////////////////

	@Override
	public ScreenshotInfo takeScreenshot() {
		return null;
	}

	///////////////////////////////////

	private int getVSyncRef(VSyncType type) {
		switch(type) {
			case DISABLED:          return VK_PRESENT_MODE_IMMEDIATE_KHR;
			case RELAXED:           return VK_PRESENT_MODE_FIFO_RELAXED_KHR;
			case ENABLED:           return VK_PRESENT_MODE_FIFO_KHR;
			case TRIPLE_BUFFERED:   return VK_PRESENT_MODE_MAILBOX_KHR;
			default: throw new RuntimeException("Invalid VSyncType");
		}
	}

	@Override
	public boolean isVSyncSupported(VSyncType type) {
		return state.validPresentModes.contains(getVSyncRef(type));
	}

	@Override
	public VSyncType getCurrentVSync() {
		switch(state.chosenPresentMode) {
			case VK_PRESENT_MODE_IMMEDIATE_KHR:     return VSyncType.DISABLED;
			case VK_PRESENT_MODE_FIFO_RELAXED_KHR:  return VSyncType.RELAXED;
			case VK_PRESENT_MODE_FIFO_KHR:          return VSyncType.ENABLED;
			case VK_PRESENT_MODE_MAILBOX_KHR:       return VSyncType.TRIPLE_BUFFERED;
			default: throw new RuntimeException("Invalid Vulkan Present Mode");
		}
	}

	@Override
	public void setVSync(VSyncType type) {
		state.chosenPresentMode = getVSyncRef(type);
	}

	////////////////////////////////////

	private ScreenType currentScreenType = ScreenType.WINDOWED;
	private Vector2i previousFrameSize = new Vector2i(ClientInput.currentWindowFrameSize);

	@Override
	public boolean isScreenSupported(ScreenType type) {
		switch(type) {
			case WINDOWED: return true;
			case BORDERLESS: return true;
			case FULLSCREEN: return false;  //TODO: Enable fullscreen via DISPLAY_KHR
			default: return false;
		}
	}

	@Override
	public ScreenType getCurrentScreen() {
		return currentScreenType;
	}

	@Override
	public void setScreenType(ScreenType type) {
		switch(type) {
			case WINDOWED:
				glfwSetWindowMonitor(state.GLFWWindow,
						0,
						0,
						0,
						previousFrameSize.x,
						previousFrameSize.y,
						GLFW_DONT_CARE
				);
				currentScreenType = ScreenType.WINDOWED;
				break;
			case BORDERLESS:
				previousFrameSize.set(ClientInput.currentWindowFrameSize);
				long primaryMonitor = glfwGetPrimaryMonitor();
				GLFWVidMode vidMode = glfwGetVideoMode(primaryMonitor);
				if(vidMode != null) {
					glfwSetWindowMonitor(state.GLFWWindow, primaryMonitor, 0, 0, vidMode.width(), vidMode.height(), vidMode.refreshRate());
				}else{
					glfwSetWindowMonitor(state.GLFWWindow,primaryMonitor,0,0,1920,1080,60);
				}
				currentScreenType = ScreenType.BORDERLESS;
				break;
			case FULLSCREEN:
				logger.Warning("Attempt to set ScreenType to Fullscreen - Invalid");
				break;
		}
	}

	////////////////////////////////////
	/// Configuration State Changing ///
	////////////////////////////////////

	/*

	@Override
	public void requestSettingsChange(RenderConfig newConfig) {

	}

	@Override
	public WorldRenderer getWorldRenderer() {
		return null;
	}

	@Override
	public DisplayHandle getDisplayHandle() {
		return null;
	}

	@Override
	public GUIRenderer getGUIRenderer() {
		return null;
	}

	@Override
	public String getShaderPostfix() {
		return null;
	}


	@Override
	public IconAtlas getBlockAtlas() {
		return null;
	}

	@Override
	public Screen getGUIConfigElements() {
		return null;
	}

	**/
}
