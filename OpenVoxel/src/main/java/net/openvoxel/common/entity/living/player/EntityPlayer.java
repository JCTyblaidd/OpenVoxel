package net.openvoxel.common.entity.living.player;

import net.openvoxel.common.entity.living.EntityLiving;
import net.openvoxel.common.util.AABB;

/**
 * Created by James on 15/09/2016.
 *
 * Player Entity Reference
 */
public abstract class EntityPlayer extends EntityLiving{

	protected AABB boundingBox;
	protected float eyeHeight;
	protected float yaw = 0;//0 = X+Direction
	protected float pitch = 0;//+ = up, - = down

	public EntityPlayer() {
		this.maxHealth = 20;
		this.currentHealth = 20;
		this.currentRegenRate = 0.05F;
		boundingBox = new AABB(-0.4,0F,-0.4,0.4,1.5,0.4);
		eyeHeight = 1.28F;
	}

	@Override
	public AABB getBoundingBox() {
		return boundingBox;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getPitch() {
		return pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public float getEyeHeight() {
		return eyeHeight;
	}

}
