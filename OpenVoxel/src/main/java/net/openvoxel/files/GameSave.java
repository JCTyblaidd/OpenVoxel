package net.openvoxel.files;

import java.io.File;

/**
 * Created by James on 02/09/2016.
 */
public class GameSave {

	private File folder;
	private File worldFolder;

	public GameSave(File folder) {
		this.folder = folder;
		if(!folder.exists()) {
			folder.mkdirs();
		}
		worldFolder = new File(folder,"worlds");
		if(!worldFolder.exists()) {
			worldFolder.mkdirs();
		}
	}

	public WorldSave getWorldSave(int dimension) {
		return new WorldSave(new File(worldFolder,"DIM-"+dimension));
	}


}
