package net.openvoxel.client.particles;

import net.openvoxel.client.textureatlas.Icon;

/**
 * Created by James on 23/09/2016.
 *
 * Base Particle Instance
 */
public abstract class Particle {

	public abstract Icon getParticleIcon();

	public abstract float getVerticality();

	public abstract float getHorizontality();

	public abstract boolean getTerminateOnCollides();

}
