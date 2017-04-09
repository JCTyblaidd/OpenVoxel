package net.openvoxel.loader.mods;

import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.api.mods.*;
import net.openvoxel.api.util.Version;
import net.openvoxel.common.event.init.ModInitEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
	private Logger modLogger;

	private Map<String,Method> crossModHandles;
	private Map<Class<? extends ModInitEvent>,Method> initHandles;

	private List<String> loadAfter;
	private List<String> loadBefore;
	private List<ModDependency> hardDependencies;

	private void deepFieldSet(Field field,Object obj,Object val) throws IllegalAccessException{
		if(field.isAccessible()) {
			field.set(obj,val);
		}else{
			field.setAccessible(true);
			field.set(obj,val);
		}
	}

	ModHandle(String type) throws Exception {
		modClass = Class.forName(type);
		try {
			modInstance = modClass.newInstance();
		}catch (Exception ex) {
			Logger crashLogger = Logger.getLogger("Mods");
			crashLogger.Severe("Failed to Instantiate Constructor");
			crashLogger.Severe("Please Ensure the constructor is public and has no parameters");
			crashLogger.StackTrace(ex);
			throw new ModLoader.ModLoadingException("Failed to Construct Mod");
		}
		modAnnotation = modClass.getAnnotation(Mod.class);
		crossModHandles = new HashMap<>();
		initHandles = new HashMap<>();
		modLogger = Logger.getLogger("Mods").getSubLogger(modAnnotation.name());
		modLogger.Info("Initialized Mod Handle Successfully");
		//Scan All Methods//
		for(Method m : modClass.getMethods()) {
			if(m.getAnnotation(CrossModCommsHandler.class) != null) {
				String key = m.getAnnotation(CrossModCommsHandler.class).value();
				crossModHandles.put(key,m);
				modLogger.Info("Enabled Cross Mod Communication Method: " + key);
			}
			if(m.getAnnotation(ModInitEventHandler.class) != null && m.getParameterTypes().length == 1) {
				Class<?> param = m.getParameterTypes()[0];
				if(ModInitEvent.class.isAssignableFrom(param)) {
					initHandles.put((Class<? extends ModInitEvent>) param,m);
					modLogger.Info("Enabled Initialization Method: " + param.getSimpleName());
				}
			}
		}
		//Scan All Fields//
		for(Field f : modClass.getDeclaredFields()) {
			if(f.getAnnotation(ModInstance.class) != null) {
				if(!Modifier.isStatic(f.getModifiers())) {
					modLogger.Warning("Mod Instance Class is Not Static");
					deepFieldSet(f,modInstance,modInstance);
				}else {
					deepFieldSet(f, null, modInstance);
				}
			}
			if(f.getAnnotation(ModLogger.class) != null && f.getType() == Logger.class) {
				if(Modifier.isStatic(f.getModifiers())) {
					deepFieldSet(f,null,modLogger);
				}else{
					deepFieldSet(f,modInstance,modLogger);
				}
			}
		}
		//Load Data//
		loadAfter = Arrays.asList(modAnnotation.loadAfter());
		loadBefore = Arrays.asList(modAnnotation.loadBefore());
		hardDependencies = Arrays.stream(modAnnotation.requiredMods()).map(ModDependency::parseDependency).collect(Collectors.toList());
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
