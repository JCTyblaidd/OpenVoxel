package net.openvoxel.client.renderer.vk;

import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.base.BaseTextRenderer;

public class VulkanGuiRenderer extends BaseGuiRenderer {

	//Buffer: CPU -> GPU {Host} (Data Stream)
	private long guiDrawStreamBuffer;

	//Buffer: CPU -> GPU {Host} (Data Stream - Updates Image)
	private long guiImageStreamBuffer;

	//Image Cache:

	public VulkanGuiRenderer() {

	}

	public void onSwapChainRecreate() {

	}

	@Override
	protected BaseTextRenderer loadTextRenderer() {

		//TODO: REPLACE WITH REAL SOLUTION
		return new BaseTextRenderer(null,null);//TODO: IMPL
	}

	@Override
	protected void preDraw() {
		//TODO: ACQUIRE FREED MEMORY
	}

	@Override
	protected void store(int offset, float x, float y, float u, float v, int RGB) {

	}

	@Override
	protected void redrawOld() {
		//TODO: RUN THE SAME COMMAND BUFFER {ENSURE VALID SUBMISSION}
	}

	@Override
	protected void createNewDraw() {
		//TODO: BUILD THE COMMAND BUFFER//
	}

	@Override
	public boolean allowDrawCaching() {
		return false;
	}
}
