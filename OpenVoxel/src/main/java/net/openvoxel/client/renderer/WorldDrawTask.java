package net.openvoxel.client.renderer;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.ClientInput;
import net.openvoxel.client.renderer.base.BaseGuiRenderer;
import net.openvoxel.client.renderer.base.BaseWorldRenderer;
import net.openvoxel.client.renderer.common.GraphicsAPI;
import net.openvoxel.client.utility.FrustumCuller;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.loader.classloader.Validation;
import net.openvoxel.server.ClientServer;
import net.openvoxel.utility.MatrixUtils;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.utility.async.AsyncQueue;
import net.openvoxel.utility.async.AsyncRunnablePool;
import net.openvoxel.utility.debug.Validate;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientChunkSection;
import net.openvoxel.world.client.ClientWorld;
import org.joml.FrustumIntersection;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WorldDrawTask implements Runnable {

	//Renderer State...
	private AsyncRunnablePool pool;
	private AsyncBarrier barrier;
	private final BaseWorldRenderer worldRenderer;
	private int width;
	private int height;

	//Dispatch barriers...
	private AsyncBarrier barrierCulling = new AsyncBarrier();
	private AsyncBarrier barrierUpdates = new AsyncBarrier();
	private AsyncQueue<ClientChunkSection> updateCalls = new AsyncQueue<>(2048,true);
	private AsyncQueue<ClientChunkSection> drawOnlyCalls = new AsyncQueue<>(2048,true);

	//Draw Target State...
	private List<CullingTask> cullingTasks = new ArrayList<>();
	private List<GenerateTask> generateTasks = new ArrayList<>();
	private long chunkOriginX = 0;
	private long chunkOriginZ = 0;
	private float playerX = 0;
	private float playerY = 0;
	private float playerZ = 0;
	private int viewDistance = 16;

	//World State..
	private EntityPlayerSP thePlayer;
	private ClientWorld theWorld;
	private Vector2f zLimitVector = new Vector2f(0.1F,1000.0F);
	private Matrix3f normalMatrix = new Matrix3f().identity();
	private Matrix4f cameraMatrix = new Matrix4f().identity();
	private Matrix4f perspectiveMatrix = new Matrix4f().identity();
	private Matrix4f frustumMatrix = new Matrix4f().identity();
	private FrustumIntersection frustumIntersect = new FrustumIntersection();

	WorldDrawTask(GraphicsAPI api, int asyncCount) {
		for(int x = -1; x < 1; x++) {
			for(int y = -1; y < 1; y++) {
				for(int z = -1; z < 1; z++) {
					cullingTasks.add(new CullingTask(x,y,z,1));
				}
			}
		}
		for(int i = 0; i < asyncCount; i++) {
			generateTasks.add(new GenerateTask(i));
		}
		worldRenderer = api.getWorldRenderer();
	}

	public void update(AsyncRunnablePool pool,ClientServer server, AsyncBarrier barrier) {
		this.pool = pool;
		this.barrier = barrier;
		width = ClientInput.currentWindowFrameSize.x;
		height = ClientInput.currentWindowFrameSize.y;
		thePlayer = server.getThePlayer();
		theWorld = server.getTheWorld();
		chunkOriginX = (long)Math.floor(thePlayer.xPos / 16.0);
		chunkOriginZ = (long)Math.floor(thePlayer.zPos / 16.0);
		worldRenderer.Setup(chunkOriginX,chunkOriginZ,theWorld);
		playerX = (float)(thePlayer.xPos - 16.0 * chunkOriginX);
		playerY = (float)thePlayer.yPos;
		playerZ = (float)(thePlayer.zPos - 16.0 * chunkOriginZ);
		viewDistance = 16;//TODO: UPDATE THESE CONSTANTS
		float FoV = 90.F;
		float aspectRatio = (float)width / (float)height;
		normalMatrix.identity().rotateX(thePlayer.getPitch()).rotateY(thePlayer.getYaw());
		cameraMatrix.set(normalMatrix).translate(-playerX,-playerY,-playerZ);
		perspectiveMatrix.identity().perspective(FoV,aspectRatio,zLimitVector.x,zLimitVector.y,true);
		frustumMatrix.set(perspectiveMatrix).mul(cameraMatrix);
		frustumIntersect.set(frustumMatrix);
	}

	@Override
	public void run() {
		//Reset all the barriers to initial state
		barrierCulling.reset(cullingTasks.size());
		barrierUpdates.reset(1);

		//Start all of the tasks...
		generateTasks.forEach(pool::addWork);
		cullingTasks.forEach(pool::addWork);

		//Wait for cull then draw to finish
		barrierCulling.awaitCompletion();
		barrierUpdates.completeTask();
		barrierUpdates.awaitCompletion();


		//Finish Self
		barrier.completeTask();
	}

	void freeAllData() {
		Logger.INSTANCE.Info("Invalidating Chunk Data is Not Yet Implemented");
	}

	private class GenerateTask implements Runnable{

		private final int AsyncID;

		private GenerateTask(int id) {
			AsyncID = id;
		}

		@Override
		public void run() {
			BaseWorldRenderer.AsyncWorldHandler handler = worldRenderer.getWorldHandlerFor(AsyncID);
			handler.Start();
			while(!barrierUpdates.isComplete()) {
				ClientChunkSection section = updateCalls.attemptNext();
				if(section != null) {
					handler.AsyncGenerate(section);
					barrierUpdates.completeTask();
				}
				section = drawOnlyCalls.attemptNext();
				if(section != null) {
					handler.AsyncDraw(section);
					barrierUpdates.completeTask();
				}
			}
			handler.Finish();
		}
	}

	private class CullingTask implements Runnable{
		private final int cullX;
		private final int cullY;
		private final int cullZ;
		private final int divide;

		private CullingTask(int x,int y,int z,int d) {
			cullX = x;
			cullY = y;
			cullZ = z;
			divide = d;
		}

		@Override
		public void run() {
			//Init	protected
			int cullSize = viewDistance / divide;
			int minX = cullX * cullSize;
			int maxX = minX + cullSize;
			int minZ = cullZ * cullSize;
			int maxZ = minZ + cullSize;
			int minY = cullY * 8;
			int maxY = minY + 8;

			//Run the culling code
			runCull(minX,maxX,minY,maxY,minZ,maxZ);

			//Finish...
			barrierCulling.completeTask();
		}

		private void runCull(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
			boolean success = frustumIntersect.testAab(
					minX,
					minY,
					minZ,
					maxX,
					maxY,
					maxZ
			);
			if(success) {
				if(minX + 1 == maxX && minY + 1 == maxY && minZ + 1 == maxZ) {
					ClientChunk chunk = theWorld.requestChunk(
							minX + chunkOriginX,
							minZ + chunkOriginZ,
							false
					);
					if(chunk != null) {
						ClientChunkSection section = chunk.getSectionAt(minY);
						if (section.isEmpty()) {
							if(section.isDirty()) {
								worldRenderer.InvalidateChunkSection(section);
								barrierUpdates.addNewTasks(1);
							}
						}else {
							if (section.isDirty()) updateCalls.add(section);
							else drawOnlyCalls.add(section);
							barrierUpdates.addNewTasks(1);
						}
					}
				}else{
					int splitX = ((maxX - minX) / 2) + minX;
					int splitY = ((maxY - minY) / 2) + minY;
					int splitZ = ((maxZ - minZ) / 2) + minZ;
					int[] xSplit = new int[]{minX,splitX,maxX};
					int[] ySplit = new int[]{minY,splitY,maxY};
					int[] zSplit = new int[]{minZ,splitZ,maxZ};
					int x_start = splitX != minX ? 0 : 1;
					int y_start = splitY != minY ? 0 : 1;
					int z_start = splitZ != minZ ? 0 : 1;
					for(int xi = x_start; xi < 2; xi++) {
						int min_x = xSplit[xi];
						int max_x = xSplit[xi+1];
						for(int yi = y_start; yi < 2; yi++) {
							int min_y = ySplit[yi];
							int max_y = ySplit[yi+1];
							for(int zi = z_start; zi < 2; zi++) {
								int min_z = zSplit[zi];
								int max_z = zSplit[zi+1];
								runCull(min_x,max_x,min_y,max_y,min_z,max_z);
							}
						}
					}
				}
			}
		}
	}

}
