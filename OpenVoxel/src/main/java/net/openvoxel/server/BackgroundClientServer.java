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
		thePlayer.xPos = 0;
		thePlayer.yPos = 140;
		thePlayer.zPos = 0;
		thePlayer.setPitch(-30.0F);
		dimensionMap.put(0,theWorld);
		//END OF DEBUG CODE
	}

	@Override
	public void run() {
		//super.run();
		thePlayer.setYaw(thePlayer.getYaw() + 0.001F);
	}
}
