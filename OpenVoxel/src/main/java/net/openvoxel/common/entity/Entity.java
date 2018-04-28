package net.openvoxel.common.entity;

import com.jc.util.format.json.JSONDouble;
import com.jc.util.format.json.JSONMap;
import com.jc.util.format.json.JSONObject;
import net.openvoxel.common.util.AABB;
import net.openvoxel.server.DedicatedServer;
import net.openvoxel.world.World;

/**
 * Created by James on 25/08/2016.
 *
 * It is a Requirement That All Class that extend Entity have a default constructor!!!
 */
public abstract class Entity {

	//Current Position//
	public double xPos;
	public double yPos;
	public double zPos;

	public double xVel;
	public double yVel;
	public double zVel;

	protected boolean dataDirty;

	public boolean markedForRemoval;

	public World currentWorld;

	public Entity() {
		xPos = 0;
		yPos = 0;
		zPos = 0;
		xVel = 0;
		yVel = 0;
		zVel = 0;
	}

	public Entity(World w, double x, double y, double z) {
		currentWorld = w;
		xPos = x;
		yPos = y;
		zPos = z;
		xVel = 0;
		yVel = 0;
		zVel = 0;
	}

	public void loadFromJSON(JSONMap<JSONObject> json) {
		xPos = json.get("x").asDouble();
		yPos = json.get("y").asDouble();
		zPos = json.get("z").asDouble();
		xVel = json.get("xv").asDouble();
		yVel = json.get("yv").asDouble();
		zVel = json.get("zv").asDouble();
	}
	public void storeToJSON(JSONMap<JSONObject> json) {
		json.put("x",new JSONDouble(xPos));
		json.put("y",new JSONDouble(yPos));
		json.put("z",new JSONDouble(zPos));
		json.put("xv",new JSONDouble(xVel));
		json.put("yv",new JSONDouble(yVel));
		json.put("zv",new JSONDouble(zVel));
	}

	/**
	 * @return Bounding Box, where (0,0,0) = (xpos,ypos,zpos)
	 */
	public abstract AABB getBoundingBox();

	/**
	 * Tick - Server Side
	 */
	public void tickServer(DedicatedServer server) {
		simulateGravity();

		updatePosition();
		if(dataDirty) {
			dataDirty = false;
			//SEND UPDATES TO CLIENTS
		}
	}

	/**
	 * Tick - Client Side (Simulating Server Data)
	 */
	public void tickClient() {
		simulateGravity();

		updatePosition();
	}

	protected void simulateGravity() {//Not Dirty: Simulated on Both Sides
		accelerate(0,-currentWorld.getGravity(),0);
	}

	protected void updatePosition() {//Not Dirty: Simulated on Both Sides
		xPos += xVel;
		yPos += yVel;
		zPos += zVel;
	}

	protected void accelerate(double x, double y, double z) {
		xVel += x;
		yVel += y;
		zVel += z;
	}

}
