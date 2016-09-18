package net.openvoxel.common.registry;

import net.openvoxel.common.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 02/09/2016.
 *
 * Entities are 100% name based
 */
public class RegistryEntities {

	private Map<Class<? extends Entity>,String> entityNameMap;
	private Map<String,Class<? extends Entity>> nameEntityMap;

	public RegistryEntities() {
		entityNameMap = new HashMap<>();
		nameEntityMap = new HashMap<>();
	}

	public void register(String ID,Class<? extends Entity> type) {
		entityNameMap.put(type,ID);
		nameEntityMap.put(ID,type);
	}

	public String getNameOfEntityClass(Class<? extends Entity> clazz) {
		return entityNameMap.get(clazz);
	}
	public Class<? extends Entity> getClassFromName(String ID) {
		return nameEntityMap.get(ID);
	}

}
