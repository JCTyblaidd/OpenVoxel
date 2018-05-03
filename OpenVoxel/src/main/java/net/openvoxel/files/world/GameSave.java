package net.openvoxel.files.world;

import com.jc.util.filesystem.FileHandle;
import com.jc.util.format.json.*;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.util.Version;
import net.openvoxel.loader.mods.ModHandle;
import net.openvoxel.loader.mods.ModLoader;
import net.openvoxel.utility.CrashReport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 02/09/2016.
 *
 * Wraps around the single game state
 */
public class GameSave {

	private File folder;
	private File worldFolder;
	private TIntObjectMap<WorldSave> loadedDimensions;

	public GameSave(File folder) {
		loadedDimensions = new TIntObjectHashMap<>();
		this.folder = folder;
		boolean exists = true;
		if(!folder.exists()) {
			folder.mkdirs();
			exists = false;
		}
		worldFolder = new File(folder,"worlds");
		if(!worldFolder.exists()) {
			worldFolder.mkdirs();
		}
		if(!exists) {
			OpenVoxel.getInstance().blockRegistry.generateMappingsFromRaw();
			//OpenVoxel.getInstance().itemRegistry TODO: implement
			saveCurrentModInformation();
			saveCurrentMappingInformation();
		}else{
			try {
				Logger validateLogger = Logger.getLogger("Save Loader").getSubLogger("Validation");
				//Validate Mods are OK//
				validateLogger.Info("Validating Mods");
				JSONList modInfo = getModInformation().asList();
				List<String> oldModIDs = new ArrayList<>();
				for(int i = 0; i < modInfo.size(); i++) {
					JSONMap modData = modInfo.get(i).asMap();
					String id = modData.get("id").asString();
					Version version = Version.parseVersion(modData.get("version").asString());
					ModHandle handle = ModLoader.getInstance().getHandleFor(id);
					if(handle == null) {
						validateLogger.Warning("Removed Mod: " + id + " : " + version.getValString());
					}else{
						Version currentVersion = Version.parseVersion(handle.getInformation().version());
						int compare = currentVersion.compareTo(version);
						if(compare == 0) {
							validateLogger.Info("Unchanged Mod: " + id + " : " + currentVersion.getValString());
						}else if(compare == 1) {
							validateLogger.Info("Updated Mod:" + id + " : " + version.getValString() + " => " + currentVersion.getValString());
						}else{
							validateLogger.Warning("Older Mod:" + id + " : " + version.getValString() + " => " + currentVersion.getValString());
						}
					}
					oldModIDs.add(id);
				}
				ModLoader.getInstance().streamMods().forEach(e -> {
					if(!oldModIDs.contains(e.getInformation().id())) {
						validateLogger.Info("Added Mod: " + e.getInformation().name() + " : " + e.getInformation().version());
					}
				});
				//Validate Mappings then load them//
				validateLogger.Info("Validating Mappings");
				JSONMap mapInfo = getCurrentMappingInformation().asMap();
				JSONMap blockMapping = mapInfo.get("block").asMap();
				for(int i = 0; i < mapInfo.size(); i++) {
					String blockName = blockMapping.getKeyAt(i).asString();
					if(OpenVoxel.getInstance().blockRegistry.getBlockFromName(blockName) == null) {
						validateLogger.Severe("Block ID Removed: " + blockName);
						CrashReport crashReport = new CrashReport("Block ID Not Present");
						OpenVoxel.reportCrash(crashReport);
					}
				}
				validateLogger.Info("Validation Complete");
				//Now Update//
				saveCurrentModInformation();
				saveCurrentMappingInformation();
			}catch (JSONType.WrongJSONTypeException ex) {
				CrashReport crashReport = new CrashReport("Invalid JSON Formatting")
						.invalidState("Validating and Loading Save Information")
						.caughtException(ex);
				OpenVoxel.reportCrash(crashReport);
			}
		}
	}

	public WorldSave getWorld(int dimension) {
		WorldSave request = loadedDimensions.get(dimension);
		if(request != null) {
			return request;
		}else {
			WorldSave save =  new WorldSave(new File(worldFolder, "DIM-" + dimension));
			loadedDimensions.put(dimension,save);
			return save;
		}
	}

	public void unloadAll(int dimension) {
		loadedDimensions.get(dimension).closeAll();
		loadedDimensions.remove(dimension);
	}

	public void saveCurrentModInformation() {
		JSONList modInfo = new JSONList();
		ModLoader.getInstance().streamMods().map(ModHandle::getInformation).forEach(info -> {
			JSONMap modData = new JSONMap();
			modData.put("id",info.id());
			modData.put("version",info.version());
			modInfo.add(modData);
		});
		String rawData = modInfo.toPrettyJSONString();
		FileHandle handle = new FileHandle(new File(folder,"mod_info.json"));
		handle.startWrite();
		handle.write(rawData);
		handle.stopWrite();
	}

	public JSONObject getModInformation() {
		FileHandle handle = new FileHandle(new File(folder,"mod_info.json"));
		return JSON.fromString(handle.getString());
	}

	private void saveCurrentMappingInformation() {
		JSONMap mappingInfo = new JSONMap();
		JSONMap blockMap = new JSONMap();
		JSONMap itemMap = new JSONMap();
		mappingInfo.put("block", blockMap);
		mappingInfo.put("item",itemMap);
		OpenVoxel.getInstance().blockRegistry.getDataMap().forEachEntry((k,v) -> {
			blockMap.put(k,v);
			return true;
		});
		FileHandle handle = new FileHandle(new File(folder,"mapping_info.json"));
		handle.startWrite();
		handle.write(mappingInfo.toPrettyJSONString());
		handle.stopWrite();
	}

	private JSONObject getCurrentMappingInformation() {
		FileHandle handle = new FileHandle(new File(folder,"mapping_info.json"));
		return JSON.fromString(handle.getString());
	}

	public void close() {
		loadedDimensions.forEachValue(e -> {
			e.closeAll();
			return true;
		});
		loadedDimensions.clear();
	}

}
