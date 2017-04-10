package net.openvoxel.files;

import net.openvoxel.utility.collection.ChunkMap;
import net.openvoxel.world.chunk.Chunk;

import java.io.File;

/**
 * Created by James on 02/09/2016.
 *
 * Wrapper around disk stored world style information
 *
 * TODO: per world information
 */
public class WorldSave {

	private File worldFolder;
	private File chunkFolder;
	private ChunkMap<ChunkSaveSection> loadedSaveSections;

	WorldSave(File folder) {
		worldFolder = folder;
		worldFolder.mkdir();
		chunkFolder = new File(worldFolder,"chunks");
		chunkFolder.mkdir();
		loadedSaveSections = new ChunkMap<>();
	}


	private ChunkSaveSection getChunkSaveSection(int chunkX, int chunkZ) {
		int sectionX = chunkX & ~31;
		int sectionZ = chunkZ & ~31;
		ChunkSaveSection section = loadedSaveSections.get(sectionX,sectionZ);
		if(section == null) {
			File loc = new File(chunkFolder,"section-"+sectionX+","+sectionZ);
			section = new ChunkSaveSection(loc);
			loadedSaveSections.set(sectionX,sectionZ,section);
		}
		return section;
	}

	public void saveChunk(Chunk chunk) {
		getChunkSaveSection(chunk.chunkX,chunk.chunkZ).saveChunk(chunk);
	}

	public boolean doesChunkExit(int chunkX, int chunkZ) {
		return getChunkSaveSection(chunkX,chunkZ).hasChunk(chunkX,chunkZ);
	}
	public Chunk loadChunk(int xCoord, int zCoord) {
		return getChunkSaveSection(xCoord,zCoord).loadChunk(xCoord,zCoord);
	}
	public void unloadChunk(int xCoord, int zCoord) {
		ChunkSaveSection section = getChunkSaveSection(xCoord, zCoord);
		section.unloadChunk(xCoord, zCoord);
		if(section.getChunkCount() == 0) {
			section.unloadAll();
			loadedSaveSections.remove(xCoord & ~31, zCoord & ~31);
		}
	}

	public void closeAll() {

	}
}
