package net.openvoxel.common.entity.living;

import net.openvoxel.common.entity.Entity;

/**
 * Created by James on 15/09/2016.
 *
 * Entity With Health
 */
public abstract class EntityLiving extends Entity{

	public float currentHealth;
	public float maxHealth;
	public float currentRegenRate;

	public EntityLiving() {
		this.maxHealth = 1;
		this.currentHealth = 1;
	}

	public void kill() {
		this.currentHealth = 0;
		this.markedForRemoval = true;
	}
}
