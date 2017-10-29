package net.openvoxel.loader.mods;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.mods.ASMHandler;
import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.api.util.Version;
import net.openvoxel.client.gui.menu.ScreenLoading;
import net.openvoxel.common.event.init.ModInitEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by James on 25/08/2016.
 *
 * Non-Classloader Style ModLoading
 */
public class ModLoader {

	private static ModLoader instance;
	private Map<String,ModHandle> loadedMods;
	private List<ModHandle> initialisationOrder;

	private List<EnabledModData> enabledModDataList;
	private long modHash;//Mod Hash For ALL Mods
	private long modHash_Client;//Mod Hash For Client Enabled Mods
	private long modHash_Server;//Mod Hash For Server Enabled Mods

	public String[] asmClasses;

	public static ModLoader getInstance() {
		return instance;
	}

	public static void Initialize(String[] classes) {
		try {
			instance = new ModLoader(classes);
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	private ModLoader(String[] args) throws Exception{
		loadedMods = new HashMap<>();
		initialisationOrder = new ArrayList<>();
		enabledModDataList = new ArrayList<>();
		for(String a : args) {
			ModHandle handle = new ModHandle(a);
			loadedMods.put(handle.getInformation().id(),handle);
		}
	}

	public static void sendModMessage(String modID,String messageType,Object... messageContent) {
		instance.getHandleFor(modID).sendCrossModMessage(messageType,messageContent);
	}

	public ModHandle getHandleFor(String ID) {
		return loadedMods.get(ID);
	}

	public int getModCount() {
		return loadedMods.size();
	}

	public static boolean isModLoaded(String modID) {
		return instance.loadedMods.keySet().contains(modID);
	}

	public static Version getModVersion(String modID) {
		return Version.parseVersion(instance.loadedMods.get(modID).getInformation().version());
	}

	public Stream<ModHandle> streamMods() {
		return loadedMods.values().stream();
	}

	private Map<String,Set<String>> getLoadAfterStr() {
		Map<String,Set<String>> res = loadedMods.keySet().stream().collect(Collectors.toMap(o -> o,v -> new HashSet<>()));
		Logger.getLogger("Mod Loader").Info("Generating Dependency Maps");
		//Load 1 Level Mapping//
		loadedMods.forEach((k,obj) -> {
			List<String> softLoadAfter = obj.getSoftLoadAfter();
			List<String> softLoadBefore = obj.getSoftLoadBefore();
			softLoadAfter.stream().filter(ModLoader::isModLoaded).forEach(loadAfter -> res.get(k).add(loadAfter));
			softLoadBefore.stream().filter(ModLoader::isModLoaded).forEach(loadBefore -> res.get(loadBefore).add(k));
		});
		//Convert to Deep Mapping//
		Logger.getLogger("Mod Loader").Info("Generating Deep Dependency Maps");
		res.forEach((mod,depend) -> {
			List<String> possibleAdd = new ArrayList<>();
			List<String> previousLevel = new ArrayList<>();
			List<String> nextLevel = new ArrayList<>();
			previousLevel.addAll(depend);
			int Lim = 0;
			while(previousLevel.size() != 0) {
				Lim++;
				if(Lim > 200) {
					throw new ModLoadingException("Circular Dependency Problem With: " + mod);
				}
				for(String prev : nextLevel) {
					Set<String> dependLevel2 = res.get(prev);
					possibleAdd.addAll(dependLevel2);
					nextLevel.addAll(dependLevel2);
				}
				List<String> temp = previousLevel;
				previousLevel = nextLevel;
				nextLevel = temp;
				previousLevel.clear();
			}
			depend.addAll(possibleAdd);
		});
		return res;
	}
	private void deepSort() {
		Map<String,Set<String>> loadAfterStr = getLoadAfterStr();
		//Convert to Mod Handles//
		Map<ModHandle,Set<ModHandle>> loadAfter = new HashMap<>();
		loadAfterStr.forEach((id,depends) -> {
			ModHandle handle = loadedMods.get(id);
			Set<ModHandle> set = depends.stream().map(loadedMods::get).collect(Collectors.toSet());
			loadAfter.put(handle,set);
		});
		Logger.getLogger("Mod Loader").Info("Starting ModHandle Load Order Sort");
		//Run Sort : should be faster due to the complete depends On//
		final int SIZE = initialisationOrder.size();
		mainSort:
		for(int i = 0; i < SIZE; i++) {
			ModHandle handle = initialisationOrder.get(i);
			Set<ModHandle> after = loadAfter.get(handle);
			for(ModHandle afterMod : after) {
				if(i == 0) {
					//Move Before//
					initialisationOrder.remove(afterMod);
					initialisationOrder.add(0,afterMod);
					//Reset w/ the same index//
					i--;
					continue mainSort;
				}else {
					if(!initialisationOrder.subList(0,i).contains(afterMod)) {
						//Move Before//
						initialisationOrder.remove(afterMod);
						initialisationOrder.add(i,afterMod);
						//Reset w/ the same index//
						i--;
						continue mainSort;
					}
				}
			}
		}
		Logger.getLogger("Mod Loader").Info("Finished Load Order Sort");
	}

	public void generateDependencyOrder() throws ModLoadingException{
		//Store to Init Order w/ alphabetical sorting//
		loadedMods.keySet().stream().sorted().map(loadedMods::get).forEach(initialisationOrder::add);
		if(loadedMods.containsKey("vanilla")) {
			ModHandle vanillaHandle = loadedMods.get("vanilla");
			initialisationOrder.remove(vanillaHandle);
			initialisationOrder.add(0,vanillaHandle);
		}
		//Ensure Versions//
		try {
			initialisationOrder.forEach(ModHandle::checkDependencyExistsAndVersion);
		}catch(ModLoadingException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		//Sort//
		deepSort();

		//Gen Enabled Mod List//
		enabledModDataList.addAll(initialisationOrder.stream().map(EnabledModData::new).collect(Collectors.toList()));
		for(String asmClz : asmClasses) {
			try {
				ASMHandler h = Class.forName(asmClz).getAnnotation(ASMHandler.class);
				if(h.isSeparateMod()) {
					enabledModDataList.add(new EnabledModData(h));
				}
			}catch(Exception e) {e.printStackTrace();}
		}
		modHash = 0;
		modHash_Client = 0;
		modHash_Server = 0;
		for(EnabledModData dat : enabledModDataList) {
			long hashCode = dat.longHash();
			modHash ^= hashCode;
			if(dat.requiresClient()) {
				modHash_Client ^= hashCode;
			}
			if(dat.requiresServer()) {
				modHash_Server ^= hashCode;
			}
		}
	}

	static class ModLoadingException extends RuntimeException {
		ModLoadingException(String str) {super(str);}
	}

	public void propagateInitEvent(ModInitEvent e,String ID,String Pre) {
		Logger log = Logger.getLogger("Mod Loader").getSubLogger(ID);
		for(ModHandle h : initialisationOrder) {
			log.Info(Pre + h.getInformation().name());
			h.propagateEvent(e);
		}
	}

	@SideOnly(side = Side.CLIENT)
	public void propagateInitEvent(ModInitEvent e, String ID, String Pre, ScreenLoading loading) {
		Logger log = Logger.getLogger("Mod Loader").getSubLogger(ID);
		for(ModHandle h : initialisationOrder) {
			log.Info(Pre + h.getInformation().name());
			loading.startMod(h.getInformation().name());
			h.propagateEvent(e);
		}
	}

	/**
	 * @return A Hash of all the loaded mods
	 */
	public long getModHash() {
		return modHash;
	}


	public long getNeedsServerModHash() {
		return modHash_Server;
	}

	public long getNeedsClientModHash() {
		return modHash_Client;
	}

}
