package net.openvoxel.loader.mods;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.mods.CrossModCommsHandler;
import net.openvoxel.api.mods.Mod;
import net.openvoxel.api.mods.ModInitEventHandler;
import net.openvoxel.api.mods.ModInstance;
import net.openvoxel.api.util.Version;
import net.openvoxel.common.event.init.ModInitEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by James on 25/08/2016.
 *
 * Mod Instance Wrapper
 */
public class ModHandle {

	private Object modInstance;
	private Class<?> modClass;
	private Mod modAnnotation;

	private Map<String,Method> crossModHandles;
	private Map<Class<? extends ModInitEvent>,Method> initHandles;

	private List<String> loadAfter;
	private List<String> loadBefore;
	private List<ModDependency> hardDependencies;

	public ModHandle(String type) throws Exception{
		modClass = Class.forName(type);
		modInstance = modClass.newInstance();
		modAnnotation = modClass.getAnnotation(Mod.class);
		crossModHandles = new HashMap<>();
		initHandles = new HashMap<>();
		//Scan All Methods//
		for(Method m : modClass.getMethods()) {
			if(m.getAnnotation(CrossModCommsHandler.class) != null) {
				String key = m.getAnnotation(CrossModCommsHandler.class).value();
				crossModHandles.put(key,m);
			}
			if(m.getAnnotation(ModInitEventHandler.class) != null && m.getParameterTypes().length == 1) {
				Class<?> param = m.getParameterTypes()[0];
				if(ModInitEvent.class.isAssignableFrom(param)) {
					initHandles.put((Class<? extends ModInitEvent>) param,m);
				}
			}
		}
		//Scan All Fields//
		for(Field f : modClass.getFields()) {
			if(f.getAnnotation(ModInstance.class) != null) {
				f.set(null,modInstance);
			}
		}
		//Load Data//
		loadAfter = Arrays.asList(modAnnotation.loadAfter());
		loadBefore = Arrays.asList(modAnnotation.loadBefore());
		hardDependencies = Arrays.asList(modAnnotation.requiredMods()).stream().map(ModDependency::parseDependency).collect(Collectors.toList());
	}

	public Object getModInstance() {
		return modInstance;
	}

	public void propagateEvent(ModInitEvent e) {
		try {
			for (Map.Entry<Class<? extends ModInitEvent>, Method> entry : initHandles.entrySet()) {
				if (entry.getKey().isAssignableFrom(e.getClass())) {
					entry.getValue().invoke(modInstance, e);
				}
			}
		}catch (Exception exe) {
			exe.printStackTrace();
		}
	}

	public void sendCrossModMessage(String type,Object[] Data) {
		try {
			crossModHandles.get(type).invoke(modInstance, Data);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Mod getInformation() {
		return modAnnotation;
	}

	public List<ModDependency> getHardDependencies() {
		return hardDependencies;
	}
	public List<String> getSoftLoadAfter() {
		return loadAfter;
	}
	public List<String> getSoftLoadBefore() {
		return loadBefore;
	}

	void checkDependencyExistsAndVersion() throws ModLoader.ModLoadingException{
		hardDependencies.forEach(ModDependency::checkExistsAndVersion);
		Version minOVoxelVer = Version.parseVersion(modAnnotation.minimumOpenVoxelVersion());
		if(OpenVoxel.currentVersion.compareTo(minOVoxelVer) == -1) {
			throw new ModLoader.ModLoadingException("Mod ID: " + modAnnotation.id() + ", requires later OpenVoxel Version: " + minOVoxelVer);
		}
	}
}
