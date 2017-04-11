package net.openvoxel.files;

import javafx.util.Pair;
import net.openvoxel.OpenVoxel;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.collection.ChunkMap;
import net.openvoxel.world.chunk.Chunk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by James on 08/04/2017.
 *
 * Wraps around the saved information of a 32x32 saved section of chunks
 */
public class ChunkSaveSection {

	private File dataFile;
	private Set<Pair<Integer,Integer>> requiredCoordinates;
	private RandomAccessFile randomAccessFile;
	private ChunkMap<Integer> offsetMap;
	private int entityOffsetMap;
	private int entityByteSize;
	private int lastChunkData;

	ChunkSaveSection(File ref) {
		this.dataFile = ref;
		requiredCoordinates = new HashSet<>();
		offsetMap = new ChunkMap<>();
		if(!this.dataFile.exists()) {
			try {
				this.dataFile.createNewFile();
			}catch (IOException error) {
				CrashReport crashReport = new CrashReport("Error Loading Chunk Section")
						                          .invalidState("Cannot Create File")
						                          .caughtException(error);
				OpenVoxel.reportCrash(crashReport);
			}
		}
		try {
			randomAccessFile = new RandomAccessFile(dataFile, "rw");
			//firstly load all offsets//
			if (randomAccessFile.length() == 0) {
				randomAccessFile.seek(0);
				for(int i = 0; i < (32*32); i++) {
					randomAccessFile.writeInt(-1);
				}
				//Entity Offset Information//
				randomAccessFile.writeInt(0);
				randomAccessFile.writeInt(0);
			}
			randomAccessFile.seek(0);
			for (int xDiff = 0; xDiff < 32; xDiff++) {
				for (int zDiff = 0; zDiff < 32; zDiff++) {
					int offset = randomAccessFile.readInt();
					offsetMap.set(xDiff,zDiff,offset);
				}
			}
			entityOffsetMap = randomAccessFile.read();
			entityByteSize = randomAccessFile.read();
		}catch (IOException error) {
			CrashReport crashReport = new CrashReport("Error Initialising Random Access File")
					.caughtException(error);
			OpenVoxel.reportCrash(crashReport);
		}
	}

	private void moveFileData(int position,int offset) throws IOException {
		randomAccessFile.seek(position);
		long len = randomAccessFile.length();
		byte[] cache_data = new byte[(int)(len - position)];
		randomAccessFile.read(cache_data);
		randomAccessFile.seek(position + offset);
		randomAccessFile.write(cache_data);
		//0 the remaining data//
		randomAccessFile.seek(position);
		for(int i = 0; i < offset; i++) {
			randomAccessFile.write(0);
		}
	}

	boolean hasChunk(int xCoord, int zCoord) {
		return offsetMap.get(xCoord,zCoord) != -1;
	}

	private ByteBuffer dataCache = ByteBuffer.allocate(16*16*256);

	Chunk loadChunk(int xCoord, int zCoord) {
		/**
		int offset = offsetMap.get(coordinate);
		try {
			if(offset < 1) {
				throw new RuntimeException("Requested Non Existing Chunk Data");
			}
			randomAccessFile.seek(offset);
			Chunk loadedChunk = new Chunk(coordinate);
			dataCache.position(0);
			randomAccessFile.read(dataCache.array());
			for(int i = 0; i < (16*16*256); i++){
				loadedChunk.BlockData[i] = dataCache.get(i);
			}
			return loadedChunk;
		}catch (IOException error) {
			CrashReport crashReport = new CrashReport("Error Loading Chunk Information")
					.caughtException(error);
			OpenVoxel.reportCrash(crashReport);
			return null;
		}
		 **/
		return null;
	}

	void saveChunk(Chunk chunk) {
		/**
		int offset = offsetMap.get(chunk.coordinate);
		if(offset < 1) {
			//Allocate Area//
		}
		 **/
	}

	void unloadChunk(int x, int z) {
		requiredCoordinates.remove(new Pair<>(x,z));
	}

	int getChunkCount() {
		return requiredCoordinates.size();
	}

	void unloadAll() {
		try {
			randomAccessFile.close();
		}catch (IOException error) {
			CrashReport crashReport = new CrashReport("Failed to Close Random Access File")
					.invalidState("Closing the file resulted in an error")
					.caughtException(error);
			OpenVoxel.reportCrash(crashReport);
		}
	}

}
