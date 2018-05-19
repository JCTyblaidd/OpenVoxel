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

	//World State
	public ClientWorld theWorld;
	public EntityPlayerSP thePlayer;

	//World Linear Algebra State
	public final Vector3f cameraVector = new Vector3f();
	public final Vector2f zLimitVector = new Vector2f(0.1F,1000.0F);
	public final Matrix3f normalMatrix = new Matrix3f().identity();
	public final Matrix4f cameraMatrix = new Matrix4f().identity();
	public final Matrix4f perspectiveMatrix = new Matrix4f().identity();
	public final Matrix4f frustumMatrix = new Matrix4f().identity();
	public final FrustumIntersection frustumIntersect = new FrustumIntersection();

	//World Shadow State
	public final Vector3f skyLightVector = new Vector3f(0,-1,0);
	public final Matrix4f[] shadowMatrixList = new Matrix4f[4];
	public final FrustumIntersection[] shadowIntersectList = new FrustumIntersection[4];
	public final Matrix4f totalShadowMatrix = new Matrix4f().identity();
	public final FrustumIntersection totalShadowIntersect = new FrustumIntersection();

	WorldDrawTask(GraphicsAPI api, int asyncCount) {
		this.asyncCount = asyncCount;
		culler = new WorldCullManager(this);
		worldRenderer = api.getWorldRenderer();
		for(int i = 0; i < 4; i++) {
			shadowMatrixList[i] = new Matrix4f().identity();
			shadowIntersectList[i] = new FrustumIntersection();
		}
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

		//Calculate Shadow Linear Algebra
	}

	void ignore() {
		worldRenderer.Setup(0,0,null);
	}

	@Override
	public void run() {

		//Initialize Work Handlers
		for(int i = 0; i < asyncCount; i++) {
			worldRenderer.SetupAsync(i);
		}

		//Run Standard World Culling
		updateCount = 0;
		barrierUpdates.reset(1);
		culler.runFrustumCull(section -> {
			if(section.isDrawDirty() && updateCount < MAX_TRANSFER_CALLS_PER_FRAME) {
				barrierUpdates.addNewTasks(1);
				updateCount += 1;

				//Mark Section Updating as Handled
				section.markDrawUpdated();
				section.Renderer_Generation.reset(1);

				//Generate And Draw World
				pool.addWork(asyncID -> {

					//Generate Section
					worldRenderer.GenerateChunkSection(section,asyncID);
					section.Renderer_Generation.completeTask();

					//Draw World
					worldRenderer.DrawWorldChunkSection(section,asyncID);
					barrierUpdates.completeTask();
				});
			}else{
				barrierUpdates.addNewTasks(1);
				pool.addWork(asyncID -> {

					//Draw World
					worldRenderer.DrawWorldChunkSection(section,asyncID);
					barrierUpdates.completeTask();
				});
			}
		});

		//Run Shadow Map World Culling
		if(worldRenderer.getShadowFrustumCount() > 0) {
			culler.runShadowCull(section -> {
				if(section.isDrawDirty() && updateCount < MAX_TRANSFER_CALLS_PER_FRAME) {
					barrierUpdates.addNewTasks(1);
					updateCount += 1;

					//Mark Section Updating as Handled
					section.markDrawUpdated();
					section.Renderer_Generation.reset(1);

					//Generate And Draw Shadow
					pool.addWork(asyncID -> {

						//Generate Section
						worldRenderer.GenerateChunkSection(section,asyncID);
						section.Renderer_Generation.completeTask();

						//Draw Shadow
						worldRenderer.DrawShadowChunkSection(section,asyncID);
						barrierUpdates.completeTask();
					});
				}else{
					barrierUpdates.addNewTasks(1);
					pool.addWork(asyncID -> {

						//Wait for Generation
						section.Renderer_Generation.awaitCompletion();

						//Draw Shadow
						worldRenderer.DrawShadowChunkSection(section,asyncID);
						barrierUpdates.completeTask();
					});
				}
			});
		}

		int nearDistance = Math.min(viewDistance,worldRenderer.getNearbyCullSize());

		//Run Nearby Map World Culling
		if(nearDistance > 0) {
			culler.runVoxelCull(nearDistance,section -> {
				if(section.isDrawDirty() && updateCount < MAX_TRANSFER_CALLS_PER_FRAME) {
					barrierUpdates.addNewTasks(1);
					updateCount += 1;

					//Mark Section Updating as Handled
					section.markDrawUpdated();
					section.Renderer_Generation.reset(1);

					//Generate and Draw Nearby
					pool.addWork(asyncID -> {
						//Generate Section
						worldRenderer.GenerateChunkSection(section,asyncID);
						section.Renderer_Generation.completeTask();

						//Draw Nearby
						worldRenderer.DrawNearbyChunkSection(section,asyncID);
						barrierUpdates.completeTask();
					});
				}else {
					barrierUpdates.addNewTasks(1);
					pool.addWork(asyncID -> {

						//Wait for Generation
						section.Renderer_Generation.awaitCompletion();

						//Draw Nearby
						section.Renderer_Generation.awaitCompletion();
						worldRenderer.DrawNearbyChunkSection(section, asyncID);
					});
				}
			});
		}

		//Queue Wait for Completion
		pool.addWork(() -> {

			//Wait for all updates to be handled
			barrierUpdates.completeTask();
			barrierUpdates.awaitCompletion();

			//Finish all asynchronous calls
			for (int i = 0; i < asyncCount; i++) {
				worldRenderer.FinishAsync(i);
			}

			//Mark World Draw as Completed
			barrier.completeTask();
		});
	}

	void freeAllData() {
		Logger.INSTANCE.Info("Invalidating Chunk Data is Not Yet Implemented");
	}
}
