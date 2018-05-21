package net.openvoxel.client.renderer;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncTaskPool;
import net.openvoxel.utility.async.BatchEventSubmitter;
import net.openvoxel.world.client.ClientWorld;
import org.joml.*;

import java.lang.Math;

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
	public final Matrix4f invFrustumMatrix = new Matrix4f().identity();
	public final FrustumIntersection frustumIntersect = new FrustumIntersection();

	//World Shadow State
	public final Vector3f skyLightVector = new Vector3f(0,-1,0);
	public final float[] shadowSplits = new float[4];
	public final Matrix4f[] shadowMatrixList = new Matrix4f[4];
	public final Matrix4f totalShadowMatrix = new Matrix4f().identity();
	public final FrustumIntersection totalShadowIntersect = new FrustumIntersection();

	//Shadow calculation Constants
	private float cascadeSplitLambda = 0.95f;


	//Calculation Data...
	private final Vector3f[] frustumCorners = new Vector3f[8];

	WorldDrawTask(GraphicsAPI api, int asyncCount) {
		this.asyncCount = asyncCount;
		culler = new WorldCullManager(this);
		worldRenderer = api.getWorldRenderer();
		for(int i = 0; i < 4; i++) {
			shadowMatrixList[i] = new Matrix4f().identity();
		}
		for(int i = 0; i < 8; i++) {
			frustumCorners[i] = new Vector3f();
		}
	}


	void update(AsyncTaskPool pool,ClientServer server, AsyncBarrier barrier,float fovDegree, int drawDist) {
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
		playerY = (float)(thePlayer.yPos + thePlayer.getEyeHeight());
		playerZ = (float)(thePlayer.zPos - 16.0 * chunkOriginZ);
		viewDistance = drawDist;

		//Player ProjectionView Data
		float FoV = (float)Math.toRadians(fovDegree);
		float aspectRatio = (float)width / (float)height;
		float xRotate = (float)Math.toRadians(thePlayer.getPitch());
		float yRotate = (float)Math.toRadians(thePlayer.getYaw());
		float zRotate = (float)Math.toRadians(180);

		//Setup Linear Algebra
		normalMatrix.identity().rotateZ(zRotate).rotateX(xRotate).rotateY(yRotate);
		cameraVector.set(0,0,1).mul(normalMatrix);
		cameraMatrix.set(normalMatrix).translate(-playerX,-playerY,-playerZ);

		//Vulkan uses Depth [0..1]
		perspectiveMatrix.identity().perspectiveLH(FoV,aspectRatio,zLimitVector.x,zLimitVector.y,true);
		frustumMatrix.set(perspectiveMatrix).mul(cameraMatrix);
		frustumMatrix.invert(invFrustumMatrix);
		frustumIntersect.set(frustumMatrix);

		//Calculate Shadow Linear Algebra
		// Based on https://developer.nvidia.com/gpugems/GPUGems3/gpugems3_ch10.html
		// Based on https://github.com/SaschaWillems/Vulkan/blob/master/examples/shadowmappingcascade/shadowmappingcascade.cpp
		int cascadeCount = worldRenderer.getShadowFrustumCount();
		float zDelta = zLimitVector.y - zLimitVector.x;
		float zRatio = zLimitVector.y / zLimitVector.x;
		for(int i = 0; i < cascadeCount; i++) {
			float p = (i + 1) / ((float)cascadeCount);
			float log = zLimitVector.x * (float)Math.pow(zRatio, p);
			float uniform = zLimitVector.x + zDelta * p;
			float d = cascadeSplitLambda * (log - uniform) + uniform;
			shadowSplits[i] = (d - zLimitVector.x) / zDelta;
		}

		//Temporary Matrix Value
		Matrix4f lightViewMatrix = new Matrix4f();

		float lastSplitDist = 0;
		for(int i = 0; i <= cascadeCount; i++) {
			float splitDist;
			if(i == cascadeCount) {
				lastSplitDist = 0.0f;
				splitDist = 1.0f;
			}else{
				splitDist = shadowSplits[i];
			}

			frustumCorners[0].set(-1.F,  1.F, -1.F);
			frustumCorners[1].set( 1.F,  1.F, -1.F);
			frustumCorners[2].set( 1.F, -1.F, -1.F);
			frustumCorners[3].set(-1.F, -1.F, -1.F);
			frustumCorners[4].set(-1.F,  1.F,  1.F);
			frustumCorners[5].set( 1.F,  1.F,  1.F);
			frustumCorners[6].set( 1.F, -1.F,  1.F);
			frustumCorners[7].set(-1.F, -1.F,  1.F);

			//Project to world space
			for(int j = 0; j < 8; j++) {
				Vector3f vector = frustumCorners[j];
				float invW = 1.F / vector.mulPositionW(invFrustumMatrix);
				vector.mul(invW);
			}

			for (int j = 0; j < 4; j++) {
				Vector3f cornerA = frustumCorners[j + 4];
				Vector3f cornerB = frustumCorners[j];
				float distX = cornerA.x - cornerB.x;
				float distY = cornerA.y - cornerB.y;
				float distZ = cornerA.z - cornerB.z;
				cornerA.set(cornerB).add(distX*splitDist,distY*splitDist,distZ*splitDist);
				cornerB.add(distX*lastSplitDist,distY*lastSplitDist,distZ*lastSplitDist);
			}

			//Update Last Split Distance
			lastSplitDist = splitDist;

			//Find frustum center
			float centerX = 0.F;
			float centerY = 0.F;
			float centerZ = 0.F;
			for (int j = 0; j < 8; j++) {
				Vector3f corner = frustumCorners[j];
				centerX += corner.x;
				centerY += corner.y;
				centerZ += corner.z;
			}
			centerX /= 8.0F;
			centerY /= 8.0F;
			centerZ /= 8.0F;

			float radius = 0.0f;
			for (int j = 0; j < 8; j++) {
				float distance = frustumCorners[j].distance(centerX,centerY,centerZ);
				radius = Math.max(radius, distance);
			}
			radius = (float)Math.ceil(radius * 16.0f) / 16.0f;

			lightViewMatrix.lookAtLH(
				centerX - (skyLightVector.x * radius),
				centerY - (skyLightVector.y * radius),
				centerZ - (skyLightVector.z * radius),
				centerX,
				centerY,
				centerZ,
				0.F,
				1.0F,
				0.F
			);

			//Update Values
			Matrix4f shadowMatrix = (i == cascadeCount) ? totalShadowMatrix : shadowMatrixList[i];
			shadowMatrix.orthoLH(
					-radius,
					radius,
					-radius,
					radius,
					0.f,
					2 * radius
			).mul(lightViewMatrix);
			if(i == cascadeCount) {
				totalShadowIntersect.set(totalShadowMatrix);
			}else{
				shadowSplits[i] = (zLimitVector.x + splitDist * zDelta) * -1.0f;
			}
		}
	}

	void ignore() {
		worldRenderer.Setup(0,0,null);
	}

	@Override
	public void run() {

		//Batch submissions together
		BatchEventSubmitter batch = new BatchEventSubmitter(pool,8);

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
				batch.addWork(asyncID -> {

					//Generate Section
					worldRenderer.GenerateChunkSection(section,asyncID);
					section.Renderer_Generation.completeTask();

					//Draw World
					worldRenderer.DrawWorldChunkSection(section,asyncID);
					barrierUpdates.completeTask();
				});
			}else{
				barrierUpdates.addNewTasks(1);
				batch.addWork(asyncID -> {

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
					batch.addWork(asyncID -> {

						//Generate Section
						worldRenderer.GenerateChunkSection(section,asyncID);
						section.Renderer_Generation.completeTask();

						//Draw Shadow
						worldRenderer.DrawShadowChunkSection(section,asyncID);
						barrierUpdates.completeTask();
					});
				}else{
					barrierUpdates.addNewTasks(1);
					batch.addWork(asyncID -> {

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
					batch.addWork(asyncID -> {
						//Generate Section
						worldRenderer.GenerateChunkSection(section,asyncID);
						section.Renderer_Generation.completeTask();

						//Draw Nearby
						worldRenderer.DrawNearbyChunkSection(section,asyncID);
						barrierUpdates.completeTask();
					});
				}else {
					barrierUpdates.addNewTasks(1);
					batch.addWork(asyncID -> {

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
		batch.flushWork();
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
