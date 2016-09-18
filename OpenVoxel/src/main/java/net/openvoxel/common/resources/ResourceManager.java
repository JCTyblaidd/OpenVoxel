package net.openvoxel.common.resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 25/08/2016.
 */
public class ResourceManager {

	private static Map<ResourceType,Map<String,ResourceHandle>> ResourceData;
	static {
		ResourceData = new HashMap<>();
		for(ResourceType t : ResourceType.values()) {
			ResourceData.put(t,new HashMap<>());
		}
	}

	public static ResourceHandle getResource(ResourceType type,String resourceID) {
		loadResource(type,resourceID);
		return ResourceData.get(type).get(resourceID);
	}

	public static ResourceHandle getImage(String res) {
		return getResource(ResourceType.IMAGE,res);
	}

	public static boolean isResourceLoaded(ResourceType type, String resourceID) {
		return ResourceData.get(type).containsKey(resourceID);
	}

	private static void loadResource(ResourceType type, String resourceID) {
		Map<String,ResourceHandle> handleMap = ResourceData.get(type);
		if(!handleMap.containsKey(resourceID)) {
			handleMap.put(resourceID,new ResourceHandle(type,resourceID));
		}
	}

	public static void markEverthingAsDirty() {
		ResourceData.forEach((t,v) -> {
			v.forEach((n,h) -> {
				h.markAsDirty();
			});
		});
	}
}
