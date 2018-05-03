package net.openvoxel.client.renderer;

import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncRunnablePool;

public class WorldDrawTask implements Runnable{

	private AsyncRunnablePool pool;
	private AsyncBarrier barrier;
	private BaseGuiRenderer guiRenderer;
	private int width;
	private int height;

	public void update(AsyncRunnablePool pool,ClientServer server, AsyncBarrier barrier, GraphicsAPI api) {
		this.pool = pool;
		this.barrier = barrier;
		guiRenderer = api.getGuiRenderer();
		width = ClientInput.currentWindowFrameSize.x;
		height = ClientInput.currentWindowFrameSize.y;
	}

	@Override
	public void run() {

	}
}
