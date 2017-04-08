package net.openvoxel.files;

import net.openvoxel.common.world.Chunk;
import net.openvoxel.common.world.ChunkCoordinate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
	private Map<ChunkCoordinate,ChunkSaveSection> loadedSaveSections;

	WorldSave(File folder) {
		worldFolder = folder;
		worldFolder.mkdir();
		chunkFolder = new File(worldFolder,"chunks");
		chunkFolder.mkdir();
		loadedSaveSections = new HashMap<>();
	}

	private ChunkCoordinate getChunkSectionCoord(ChunkCoordinate coordinate) {
		int sectionX = coordinate.X & ~31;
		int sectionZ = coordinate.Z & ~31;
		return new ChunkCoordinate(sectionX,sectionZ);
	}

	private ChunkSaveSection getChunkSaveSection(ChunkCoordinate coordinate) {
		int sectionX = coordinate.X & ~31;
		int sectionZ = coordinate.Z & ~31;
		ChunkCoordinate sectionCoord =  new ChunkCoordinate(sectionX,sectionZ);
		ChunkSaveSection section = loadedSaveSections.get(sectionCoord);
		if(section == null) {
			File loc = new File(chunkFolder,"section-"+sectionX+","+sectionZ);
			section = new ChunkSaveSection(loc);
			loadedSaveSections.put(sectionCoord,section);
		}
		return section;
	}

	public void saveChunk(Chunk chunk) {
		getChunkSaveSection(chunk.coordinate).saveChunk(chunk);
	}

	public boolean doesChunkExit(ChunkCoordinate chunk) {
		return getChunkSaveSection(chunk).hasChunk(chunk);
	}
	public Chunk loadChunk(ChunkCoordinate chunk) {
		return getChunkSaveSection(chunk).loadChunk(chunk);
	}
	public void unloadChunk(ChunkCoordinate chunk) {
		ChunkSaveSection section = getChunkSaveSection(chunk);
		section.unloadChunk(chunk);
		if(section.getChunkCount() == 0) {
			section.unloadAll();
			loadedSaveSections.remove(getChunkSectionCoord(chunk));
		}
	}

	public void closeAll() {

	}
}
