package net.openvoxel.world.client;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.world.World;
import net.openvoxel.world.generation.IWorldGenerator;

/**
 * Created by James on 09/04/2017.
 *
 * Client Side World Implementation
 */
@SideOnly(side = Side.CLIENT)
public class ClientWorld extends World {

	public ClientWorld(IWorldGenerator generator) {
		super(generator);
	}

	@Override
	public ClientChunk requestChunk(long x, long z,boolean generate) {
		ClientChunk res = (ClientChunk)chunkMap.get((int)x,(int)z);
		if(res == null && generate) {
			res = (ClientChunk) generator.generateChunk((int)x,(int)z);
			chunkMap.set((int)x,(int)z,res);
		}//TODO: improve
		return res;
	}

	@Override
	public void releaseAllChunkData() {
		chunkMap.forEachChunk(e -> {
			//Renderer.renderer.getWorldRenderer().onChunkUnloaded((ClientChunk)e);
			e.releaseData();
			return true;
		});
		chunkMap.emptyAll();
	}

}
