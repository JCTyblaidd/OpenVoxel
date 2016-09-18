package net.openvoxel.loader.mods;

import com.jc.util.stream.ArrayUtils;
import net.openvoxel.api.mods.IASMHandler;
import net.openvoxel.loader.classloader.TweakableClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by James on 25/08/2016.
 */
public class ModDataLoader {

	private List<File> files;
	private List<ModDataHandle> handles;

	public ModDataLoader() {
		files = new ArrayList<>();
		handles = new ArrayList<>();
	}

	public void scanDirectoryForMods(File folder) {
		_recursiveScan(folder);
	}
	private void _recursiveScan(File file) {
		if(file.isFile()) {
			if(file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
				files.add(file);
			}
		}
		if(file.isDirectory()) {
			for(File sub : file.listFiles()) {
				_recursiveScan(sub);
			}
		}
	}

	public void scanLoadedMods() {
		handles.addAll(files.stream().map(ModDataHandle::new).collect(Collectors.toList()));//Map to DataHandles
		handles.forEach(ModDataHandle::ScanModFile);//Scan The Mod File
		handles.forEach(TweakableClassLoader.INSTANCE::registerSource);//Register As Loadable Location
	}

	public String[] getASMModifierList() {
		List<String> vList = new ArrayList<>();
		for(ModDataHandle h : handles) {
			vList.addAll(h.asmHandlers());
		}
		return vList.toArray(new String[vList.size()]);
	}
	public String[] getClassList() {
		List<String> vList = new ArrayList<>();
		for(ModDataHandle h : handles) {
			vList.addAll(h.modHandlers());
		}
		return vList.toArray(new String[vList.size()]);
	}
	public int getLoadedModCount() {
		return files.size();
	}

	public void handleASMDependencies(String[] asmHandles) {
		try {
			for (String a : asmHandles) {
				Class<? extends IASMHandler> clz = (Class<? extends IASMHandler>) Class.forName(a);
				IASMHandler Handle = clz.newInstance();
				TweakableClassLoader.IASMTransformer[] transformers = Handle.getASMTransformers();
				TweakableClassLoader.IBytecodeSource[] sources =  Handle.getBytecodeSources();
				ArrayUtils.Iterate(transformers).forEach(TweakableClassLoader.INSTANCE::registerTransformer);
				ArrayUtils.Iterate(sources).forEach(TweakableClassLoader.INSTANCE::registerSource);
			}
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
