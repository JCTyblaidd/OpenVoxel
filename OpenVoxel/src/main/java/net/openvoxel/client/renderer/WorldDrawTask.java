package net.openvoxel.client.renderer;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncQueue;
import net.openvoxel.utility.async.AsyncTaskPool;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldDrawTask implements Runnable {

	//Renderer State...
	private AsyncTaskPool pool;
	private AsyncBarrier barrier;
	private final BaseWorldRenderer worldRenderer;
	private int width;
	private int height;

	//Configuration
	public static final int MAX_TRANSFER_CALLS_PER_FRAME = 8;

	//Utility Classes....
	private final WorldCullManager culler;

	//Dispatch barriers...
	private AsyncBarrier barrierUpdates = new AsyncBarrier();
	private AsyncBarrier barrierGenerate = new AsyncBarrier();
	private AsyncQueue<ClientChunkSection> updateCalls = new AsyncQueue<>(2048,true);
	private AsyncQueue<ClientChunkSection> drawOnlyCalls = new AsyncQueue<>(2048,true);
	private AtomicInteger numUpdates = new AtomicInteger(0);

	//Draw Target State...
	private List<GenerateTask> generateTasks = new ArrayList<>();
	long chunkOriginX = 0;
	long chunkOriginZ = 0;
	public float playerX = 0;
	public float playerY = 0;
	public float playerZ = 0;
	public int viewDistance = 16;

	//World State..
	public ClientWorld theWorld;
	public EntityPlayerSP thePlayer;
	public Vector3f cameraVector = new Vector3f();
	public Vector2f zLimitVector = new Vector2f(0.1F,1000.0F);
	public Matrix3f normalMatrix = new Matrix3f().identity();
	public Matrix4f cameraMatrix = new Matrix4f().identity();
	public Matrix4f perspectiveMatrix = new Matrix4f().identity();
	public Matrix4f frustumMatrix = new Matrix4f().identity();
	public FrustumIntersection frustumIntersect = new FrustumIntersection();

	WorldDrawTask(GraphicsAPI api, int asyncCount) {
		culler = new WorldCullManager(this);
		for(int i = 0; i < asyncCount; i++) {
			generateTasks.add(new GenerateTask(i));
		}
		worldRenderer = api.getWorldRenderer();
	}

	void update(AsyncTaskPool pool,ClientServer server, AsyncBarrier barrier) {
		this.pool = pool;
		this.barrier = barrier;
		width = ClientInput.currentWindowFrameSize.x;
		height = ClientInput.currentWindowFrameSize.y;
		thePlayer = server.getThePlayer();
		theWorld = server.getTheWorld();

		//World Data
		chunkOriginX = (long)Math.floor(thePlayer.xPos / 16.0);
		chunkOriginZ = (long)Math.floor(thePlayer.zPos / 16.0);
		worldRenderer.Setup(chunkOriginX,chunkOriginZ,theWorld);

		//Player Draw Data
		playerX = (float)(thePlayer.xPos - 16.0 * chunkOriginX);
		playerY = (float)thePlayer.yPos + 10.F;//TODO: ADD CAMERA OFFSET
		playerZ = (float)(thePlayer.zPos - 16.0 * chunkOriginZ);
		viewDistance = 4;//TODO: UPDATE THESE CONSTANTS

		//Player ProjectionView Data
		float FoV = (float)Math.toRadians(100.F);
		float aspectRatio = (float)width / (float)height;
		float xRotate = (float)Math.toRadians(-160.0F);//TODO: USE PLAYER YAW / PITCH VALUES
		float yRotate = (float)Math.toRadians(0.0F);

		//Setup Linear Algebra
		normalMatrix.identity().rotateX(xRotate).rotateY(yRotate);
		cameraVector.set(0,0,1).rotateX(xRotate).rotateY(yRotate);
		cameraMatrix.set(normalMatrix).translate(-playerX,-playerY,-playerZ);
		perspectiveMatrix.identity().perspective(FoV,aspectRatio,zLimitVector.x,zLimitVector.y,true);
		frustumMatrix.set(perspectiveMatrix).mul(cameraMatrix);
		frustumIntersect.set(frustumMatrix);
	}

	void ignore() {
		worldRenderer.Setup(0,0,null);
	}

	@Override
	public void run() {

		//TODO: MOVE AWAY FROM SYNCHRONOUS TESTING CODE!!!

		for(int i = 0; i < generateTasks.size(); i++) {
			worldRenderer.getWorldHandlerFor(i).Start();
		}

		BaseWorldRenderer.AsyncWorldHandler handler = worldRenderer.getWorldHandlerFor(0);

		//System.out.println("START OF DRAW");
/*
		culler.runFrustumCull(section -> {
			System.out.println("DRAW @ (" + section.getChunkX()+","+section.getChunkY()+","+section.getChunkZ()+")");
			if(section.isDrawDirty()) {
				handler.AsyncGenerate(section);
			}
			handler.AsyncDraw(section);
		});

*/

		AtomicInteger limit = new AtomicInteger(0);
		culler.runFrustumCull(section -> {
			if(section.isDrawDirty() && limit.getAndIncrement() < MAX_TRANSFER_CALLS_PER_FRAME) {
				handler.AsyncGenerate(section);
				handler.AsyncDraw(section);
			}else if(!section.isDrawDirty()) {
				handler.AsyncDraw(section);
			}
		});

		//System.out.println("END OF DRAW");
/*
		int limit = 0;
		for(int x = 7; x < 10; x++) {
			for(int z = 7; z < 10; z++) {
				ClientChunk _chunk = theWorld.requestChunk(x, z, false);
				if (_chunk == null) throw new RuntimeException("Failed miserably!");
				for (int y = 0; y < 16; y++) {
					ClientChunkSection section = _chunk.getSectionAt(y);
					if (section.isDrawDirty() && limit < MAX_TRANSFER_CALLS_PER_FRAME) {
						handler.AsyncGenerate(section);
						limit++;
					}
					handler.AsyncDraw(section);
				}
			}
		}*/


		for(int i = 0; i < generateTasks.size(); i++) {
			worldRenderer.getWorldHandlerFor(i).Finish();
		}

		barrier.completeTask();

		/*
		//Reset all the barriers to initial state
		barrierGenerate.reset(generateTasks.size());
		barrierUpdates.reset(1);
		numUpdates.set(0);

		//Start all of the tasks...
		generateTasks.forEach(pool::addWork);

		//Asynchronously wait till completion...
		pool.addWork(() -> {

			//Wait for cull then draw to finish
			barrierUpdates.completeTask();
			barrierUpdates.awaitCompletion();
			//Wait for generation to finish...
			barrierGenerate.awaitCompletion();

			//Finish Self
			barrier.completeTask();
		});
		*/
	}

	void freeAllData() {
		Logger.INSTANCE.Info("Invalidating Chunk Data is Not Yet Implemented");
	}

	private class GenerateTask implements Runnable {

		private final int AsyncID;

		private GenerateTask(int id) {
			AsyncID = id;
		}

		@Override
		public void run() {
			BaseWorldRenderer.AsyncWorldHandler handler = worldRenderer.getWorldHandlerFor(AsyncID);
			handler.Start();
			ClientChunkSection section;
			while(!barrierUpdates.isComplete()) {
				section = updateCalls.attemptNext();
				if(section != null) {
					handler.AsyncGenerate(section);
					handler.AsyncDraw(section);
					barrierUpdates.completeTask();
				}
				section = drawOnlyCalls.attemptNext();
				if(section != null) {
					handler.AsyncDraw(section);
					barrierUpdates.completeTask();
				}
				try{//TODO: FIND SYNC ISSUE (AsyncQueue Broken??!)
					Thread.sleep(1);
				}catch(Exception ignore) {}
			}
			handler.Finish();
			barrierGenerate.completeTask();
		}
	}
}
