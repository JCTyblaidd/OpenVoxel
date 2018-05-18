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
		ClientChunk res = (ClientChunk)chunkMap.get(x,z);
		if(res == null && generate) {
			res = (ClientChunk)generator.generateChunk((int)x,(int)z);
			chunkMap.put(x,z,res);
		}
		return res;
	}

	//TODO: OVERRIDE RELASE CHUNK DATA & UNLOADING !!!
	/*@Override
	public void releaseAllChunkData() {
		chunkMap.forEachChunk(e -> {
			//Renderer.renderer.getWorldRenderer().onChunkUnloaded((ClientChunk)e);
			e.releaseData();
			return true;
		});
		chunkMap.emptyAll();
	}*/

}
