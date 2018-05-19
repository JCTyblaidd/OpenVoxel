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
	private final int asyncCount;

	//Utility Classes....
	private final WorldCullManager culler;

	//Dispatch barriers...
	private AsyncBarrier barrierUpdates = new AsyncBarrier();
	private int updateCount;

	//Draw Target State...
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

	//World Shadow State..
	public Vector3f skyLightVector = new Vector3f(0,-1,0);

	WorldDrawTask(GraphicsAPI api, int asyncCount) {
		this.asyncCount = asyncCount;
		culler = new WorldCullManager(this);
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
		viewDistance = 16;//TODO: UPDATE THESE CONSTANTS

		//Player ProjectionView Data
		float FoV = (float)Math.toRadians(100.F);
		float aspectRatio = (float)width / (float)height;
		float xRotate = (float)Math.toRadians(thePlayer.getPitch());
		float yRotate = (float)Math.toRadians(thePlayer.getYaw());
		float zRotate = (float)Math.toRadians(180);

		//Setup Linear Algebra
		normalMatrix.identity().rotateZ(zRotate).rotateX(xRotate).rotateY(yRotate);
		cameraVector.set(0,-1,0).mul(normalMatrix);
		cameraMatrix.set(normalMatrix).translate(-playerX,-playerY,-playerZ);
		perspectiveMatrix.identity().perspective(FoV,aspectRatio,zLimitVector.x,zLimitVector.y,false);//TODO: YES/NO??
		frustumMatrix.set(perspectiveMatrix).mul(cameraMatrix);
		frustumIntersect.set(frustumMatrix);
	}

	void ignore() {
		worldRenderer.Setup(0,0,null);
	}

	@Override
	public void run() {

		//Reset
	/*
		barrierGenerate.reset(generateTasks.size());
		numUpdates.set(0);
		queuedUpdates.clear();

		//Generate Tasks
		culler.runFrustumCull(queuedUpdates::add);

		//Dispatch Tasks
		int LIM = queuedUpdates.size() / generateTasks.size();
		int genLimit = generateTasks.size() - 1;
		int start = 0;
		for(int i = 0; i < genLimit; i++) {
			int end = start + LIM;
			generateTasks.get(i).setup(start,end);
			start = end;
		}
		generateTasks.get(genLimit).setup(start,queuedUpdates.size());

		generateTasks.forEach(pool::addWork);

		barrierGenerate.awaitCompletion();
*/


		for(int i = 0; i < asyncCount; i++) {
			worldRenderer.getWorldHandlerFor(i).Start();
		}

		/*
		updateCount = 0;
		barrierUpdates.reset(1);
		culler.runFrustumCull(section -> {
			if(section.isDrawDirty()) {
				if(updateCount < MAX_TRANSFER_CALLS_PER_FRAME) {
					barrierUpdates.addNewTasks(1);
					pool.addWork(asyncID -> {
						BaseWorldRenderer.AsyncWorldHandler handler = worldRenderer.getWorldHandlerFor(asyncID);
						handler.AsyncGenerate(section);
						handler.AsyncDraw(section);
						barrierUpdates.completeTask();
					});
				}
				updateCount += 1;
			}else{
				barrierUpdates.addNewTasks(1);
				pool.addWork(asyncID -> {
					worldRenderer.getWorldHandlerFor(asyncID).AsyncDraw(section);
					barrierUpdates.completeTask();
				});
			}
		});
		barrierUpdates.completeTask();
		*/


		BaseWorldRenderer.AsyncWorldHandler handler = worldRenderer.getWorldHandlerFor(0);

		AtomicInteger limit = new AtomicInteger(0);
		culler.runFrustumCull(section -> {
			if(section.isDrawDirty() && limit.getAndIncrement() < MAX_TRANSFER_CALLS_PER_FRAME) {
				handler.AsyncGenerate(section);
				handler.AsyncDraw(section);
			}else if(!section.isDrawDirty()) {
				handler.AsyncDraw(section);
			}
		});


		for(int i = 0; i < asyncCount; i++) {
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
}
