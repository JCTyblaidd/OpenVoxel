package net.openvoxel.files;

import net.openvoxel.common.world.Chunk;

import java.io.File;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Created by James on 02/09/2016.
 */
public class WorldSave {

	private File worldFolder;
	private File chunkFolder;
	private Deflater deflater;
	private Inflater inflater;

	WorldSave(File folder) {
		worldFolder = folder;
		worldFolder.mkdir();
		chunkFolder = new File(worldFolder,"chunks");
		chunkFolder.mkdir();
		deflater = new Deflater(Deflater.BEST_COMPRESSION);
		inflater = new Inflater();
	}

	private byte[] compress(byte[] input) {
		deflater.setInput(input);

		return null;
	}
	private byte[] decompress(byte[] input) {
		inflater.setInput(input);
		return null;
	}

	public void saveChunk(Chunk chunk) {

	}

	public boolean hasChunkSaved(Chunk chunk) {
		return false;
	}
	public Chunk loadChunk(Chunk chunk) {
		return null;
	}
}
