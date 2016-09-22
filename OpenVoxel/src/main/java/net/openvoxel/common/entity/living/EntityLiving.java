package net.openvoxel.common.entity.living;

import com.jc.util.format.json.JSONFloat;
import com.jc.util.format.json.JSONMap;
import com.jc.util.format.json.JSONObject;
import com.jc.util.format.json.JSONString;
import net.openvoxel.common.entity.Entity;
import net.openvoxel.server.Server;

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

	private void regen() {
		float v1 = currentHealth + currentRegenRate;
		if(v1 > maxHealth) {
			currentHealth = maxHealth;
		}else{
			currentHealth = v1;
		}
	}

	@Override
	public void tickClient() {
		super.tickClient();
		regen();
	}

	@Override
	public void tickServer(Server server) {
		super.tickServer(server);
		regen();
	}

	@Override
	public void storeToJSON(JSONMap<JSONObject> json) {
		super.storeToJSON(json);
		json.put(new JSONString("entity.health"),new JSONFloat(currentHealth));
		json.put(new JSONString("entity.regen"),new JSONFloat(currentRegenRate));
		json.put(new JSONString("entity.max-health"),new JSONFloat(currentHealth));
	}
}
