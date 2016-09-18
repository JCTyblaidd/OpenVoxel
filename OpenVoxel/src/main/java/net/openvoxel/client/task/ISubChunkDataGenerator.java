package net.openvoxel.client.task;

/**
 * Created by James on 02/09/2016.
 */
public interface ISubChunkDataGenerator {

	void generate(BaseChunkRenderTask chunk, int subChunk);

}
