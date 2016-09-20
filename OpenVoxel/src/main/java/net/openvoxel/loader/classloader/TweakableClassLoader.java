package net.openvoxel.loader.classloader;


import net.openvoxel.api.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by James on 31/07/2016.
 *
 * Controllable Class Wrapper
 */
public class TweakableClassLoader extends ClassLoader{

	public static TweakableClassLoader INSTANCE;

	private List<IASMTransformer> transformers;
	private List<IBytecodeSource> sources;
	private Class<?> loaderClass;

	public void unregisterAllTransformers() {
		transformers.clear();
	}

	public static Stream<IBytecodeSource> iterateSources() {
		return INSTANCE.sources.stream();
	}

	public static void Load() {
		System.out.println("==Loading ClassLoader==");
		INSTANCE = new TweakableClassLoader();
		Thread.currentThread().setContextClassLoader(INSTANCE);
	}

	private TweakableClassLoader() {
		transformers = new ArrayList<>();
		sources = new ArrayList<>();
	}

	public void registerTransformer(IASMTransformer transformer) {
		INSTANCE.transformers.add(transformer);
	}

	public void registerSource(IBytecodeSource source) {
		sources.add(source);
	}

	public int getTransformerCount() {
		return transformers.size();
	}

	public interface IASMTransformer {
		byte[] transform(byte[] values, String classID);
	}

	public interface IBytecodeSource {
		boolean contains(String classID);
		InputStream getStream(String classID) throws IOException;
	}

	//Resource Loader//

	@Override
	public InputStream getResourceAsStream(String name) {
		try{
			InputStream stream = ClassLoader.getSystemResourceAsStream(name);
			if(stream != null) {
				return stream;
			}
		}catch(Exception e) {}
		//Iterate Through ByteCode Sources//
		for(IBytecodeSource source : sources) {
			try{
				if(source.contains(name)) {
					InputStream stream = source.getStream(name);
					if(stream != null) {
						return stream;
					}
				}
			}catch(Exception e) {}
		}
		return null;
	}

	//ClassLoader//


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if(forceSystem(name)) {
			return getSystemClassLoader().loadClass(name);
		}
		try {
			return findClass(name);
		}catch (Exception e) {
			try {
				Logger.INSTANCE.Severe("Error Loading Class[Revert to System]: " + name);
			}catch(Exception e2) {}
			return getSystemClassLoader().loadClass(name);
		}
	}

	private boolean forceSystem(String classID) {
		if(classID.startsWith("sun.")) return true;
		return false;
	}

	private boolean allowTweaks(String classID) {
		if(classID.startsWith("net.openvoxel.loader.")) return false;
		//if(classID.startsWith("sun.")) return false;  //Sun stuff is skipped altogether
		if(classID.startsWith("java.")) return false;
		return true;
	}


	private Class<?> generateClass(byte[] data, String classID) {
		if(allowTweaks(classID)) {
			for (IASMTransformer transformer : transformers) {
				data = transformer.transform(data, classID);
			}
		}
		return defineClass(classID,data,0,data.length);
	}


	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);
		if(c == null) {
			/**
			try{
				//Try Get System Class//
				InputStream in = getSystemResourceAsStream(name.replace(".","/")+".class");
				byte[] arr = new byte[in.available()];
				in.read(arr);
				in.close();
				return defineClass(name,arr,0,arr.length);
			}catch(Exception e) {}
			 **/
			//Try Java Class//
			if(name.startsWith("java.") || name.startsWith("javax.")) {
				return getSystemClassLoader().loadClass(name);
			}
			//Ok Now Normal Loading//
			InputStream in_def;
			try {
				InputStream inputStream = getSystemResourceAsStream(name.replace(".", "/") + ".class");
				//in_def = new FileInputStream(new File(name.replace(".","/") + ".class"));

				//Fixes bug with reading of system resource stream that can corrupt the class file result//
				ByteArrayOutputStream output = new ByteArrayOutputStream(inputStream.available());
				int n;
				byte[] buffer = new byte[4096];
				while ( (n = inputStream.read(buffer)) != -1)
				{
					output.write(buffer, 0, n);
				}
				output.close();
				in_def = new ByteArrayInputStream(output.toByteArray());
			}catch(Exception e) {
				throw new ClassNotFoundException();
			}
			if(in_def == null) {
				//Try Additional Handlers//
				IBytecodeSource handle = null;
				for(IBytecodeSource source : sources) {
					if(source.contains(name)) {
						handle = source;
						break;
					}
				}
				if(handle == null) {
					throw new ClassNotFoundException();
				}
				try {
					in_def = handle.getStream(name);
				}catch(Exception e) {
					System.err.println("Error Getting Stream: " + name);
					throw new ClassNotFoundException();
				}
			}
			try {
				byte[] arr = new byte[in_def.available()];
				in_def.read(arr);
				in_def.close();
				c = generateClass(arr,name);
			}catch(Exception e) {
				System.err.println("Error Generating: " + name);
				e.printStackTrace();
				throw new ClassNotFoundException();
			}
		}
		return c;
	}
}
