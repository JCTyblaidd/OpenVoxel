package net.openvoxel.server;

import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.networking.ClientNetworkHandler;
import net.openvoxel.world.World;
import net.openvoxel.world.client.ClientWorld;
import net.openvoxel.world.generation.DebugWorldGenerator;

/**
 * Created by James on 10/04/2017.
 *
 * Screen Background Client Server : automatically rotates the player
 */
public class BackgroundClientServer extends ClientServer{

	public BackgroundClientServer() {
		thePlayer = null;
		serverConnection = new ClientNetworkHandler();
		//DEBUG CODE TODO: remove and update
		thePlayer = new EntityPlayerSP();
		World theWorld = new ClientWorld(new DebugWorldGenerator());
		thePlayer.currentWorld = theWorld;
		thePlayer.xPos = 100;
		thePlayer.yPos = 105;
		thePlayer.zPos = 100;
		thePlayer.setPitch(0);
		thePlayer.setYaw(0);
		dimensionMap.put(0,theWorld);
		//END OF DEBUG CODE
	}

	@Override
	public void run() {
		super.run();
		thePlayer.setYaw(thePlayer.getYaw() + 0.002F);
		/*
		if(EntityPlayerSP.keyForward.isDownRaw()) {
			thePlayer.zPos -= 0.5F;
		}
		if(EntityPlayerSP.keyBackward.isDownRaw()) {
			thePlayer.zPos += 0.5F;
		}
		if(EntityPlayerSP.keyLeft.isDownRaw()) {
			thePlayer.xPos -= 0.5F;
		}
		if(EntityPlayerSP.keyRight.isDownRaw()) {
			thePlayer.xPos += 0.5F;
		}
		if(EntityPlayerSP.keyJump.isDownRaw()) {
			thePlayer.yPos += 0.5F;
		}
		if(EntityPlayerSP.keyCrouch.isDownRaw()) {
			thePlayer.yPos -= 0.5F;
		}
		float a = (float)ClientInput.unhandledMouseDelta.x;
		float b = (float)ClientInput.unhandledMouseDelta.y;
		ClientInput.unhandledMouseDelta.x = 0;
		ClientInput.unhandledMouseDelta.y = 0;
		a /= 300;
		b /= 300;
		thePlayer.setYaw(thePlayer.getYaw() + a);
		thePlayer.setPitch(thePlayer.getPitch() + b);
		*/
	}

}
