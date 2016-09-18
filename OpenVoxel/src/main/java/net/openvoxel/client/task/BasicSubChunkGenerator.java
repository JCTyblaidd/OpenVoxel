package net.openvoxel.client.task;

import net.openvoxel.client.renderer.generic.WorldRenderer;

/**
 * Created by James on 02/09/2016.
 *
 * Async Data Generation
 */
public abstract class BasicSubChunkGenerator implements ISubChunkDataGenerator{

	abstract WorldRenderer.WorldBlockRenderer getRenderer();
	abstract void RenderFinished(BaseChunkRenderTask Data, int subChunk);

	@Override
	public void generate(BaseChunkRenderTask Data, int subChunk) {
		WorldRenderer.WorldBlockRenderer blockRenderer = getRenderer();
		BaseChunkRenderTask.RenderBlockAccess blockAccess = Data.createAccess(0,0,0);
		//Lock All 4 Side Chunks//
		final int yMin = subChunk*16;
		final int yMax = yMin+16;
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = yMin; y < yMax; y++) {
					//Handle Block//
					blockAccess.setPosition(x,y,z);
					blockAccess.getBlock().getRenderHandler().storeBlockData(blockRenderer,blockAccess);
				}
			}
		}
		RenderFinished(Data,subChunk);
	}

}
