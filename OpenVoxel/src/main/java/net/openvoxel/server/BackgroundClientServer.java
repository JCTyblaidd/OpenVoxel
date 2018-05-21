package net.openvoxel.server;

import net.openvoxel.client.ClientInput;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.utility.MathUtilities;
import net.openvoxel.utility.async.AsyncBarrier;
import net.openvoxel.world.client.ClientWorld;
import net.openvoxel.world.generation.DebugWorldGenerator;

/**
 * Created by James on 10/04/2017.
 *
 * Screen Background Client Server : automatically rotates the player
 */
public class BackgroundClientServer extends ClientServer {

	private static final float ADVANCE_RATE = 0.1F;

	public BackgroundClientServer() {
		thePlayer = new EntityPlayerSP();
		thePlayer.currentWorld = new ClientWorld(new DebugWorldGenerator());
		thePlayer.xPos = 115;
		thePlayer.yPos = 105;
		thePlayer.zPos = 115;
		thePlayer.setPitch(0.F);
		thePlayer.setYaw(0.F);
		for(int x = -8; x < 24; x++) {
			for(int z = -8; z < 24; z++) {
				thePlayer.currentWorld.requestChunk(x,z,true);
			}
		}
		dimensionMap.put(0,thePlayer.currentWorld);
	}


	@Override
	protected void executeServerTick(AsyncBarrier barrier) {
		super.executeServerTick(barrier);
		float newYaw = thePlayer.getYaw() + 5*ADVANCE_RATE;
		float newPitch = (float)(  Math.sin(Math.toRadians(newYaw)));
		thePlayer.setYaw(newYaw % 360.F);
		thePlayer.setPitch(newPitch * 90.F);
		//debug_tick();
	}

	private void debug_tick() {
		/*
		float cos = (float)Math.cos(thePlayer.getYaw());
		float sin = (float)Math.sin(thePlayer.getYaw());
		float cosP = (float)Math.cos(thePlayer.getPitch());
		float sinP = (float)Math.cos(thePlayer.getPitch());
		if(EntityPlayerSP.keyForward.isDownRaw()) {
			thePlayer.xPos += cosP * sin * 1.0F;
			thePlayer.zPos += cosP * cos * 1.0F;
			thePlayer.yPos += sinP * 1.0F;
		}
		if(EntityPlayerSP.keyBackward.isDownRaw()) {
			thePlayer.xPos += cosP * sin * -1.0F;
			thePlayer.zPos += cosP * cos * -1.0F;
			thePlayer.yPos += sinP * -1.0F;
		}
		if(EntityPlayerSP.keyLeft.isDownRaw()) {
			thePlayer.xPos += cos * 1.0F;
			thePlayer.zPos += sin * 1.0F;
		}
		if(EntityPlayerSP.keyRight.isDownRaw()) {
			thePlayer.xPos += cos * -1.0F;
			thePlayer.zPos += sin * -1.0F;
		}
		if(EntityPlayerSP.keyJump.isDownRaw()) {
			thePlayer.yPos += 0.5F;
		}
		if(EntityPlayerSP.keyCrouch.isDownRaw()) {
			thePlayer.yPos -= 0.5F;
		}*/
		float sf = 0.1F;
		float dx = (float)ClientInput.unhandledMouseDelta.x * sf;
		float dy = (float)ClientInput.unhandledMouseDelta.y * sf;
		ClientInput.resetMouseDelta();

		thePlayer.setYaw((thePlayer.getYaw() + dx + 360) % 360.F);
		thePlayer.setPitch(MathUtilities.clamp(thePlayer.getPitch() + dy,-90,90));
	}

}
