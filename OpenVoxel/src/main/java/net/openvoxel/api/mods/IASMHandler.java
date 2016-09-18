package net.openvoxel.api.mods;

import net.openvoxel.loader.classloader.TweakableClassLoader;

/**
 * Created by James on 25/08/2016.
 */
public interface IASMHandler {

	TweakableClassLoader.IBytecodeSource[] getBytecodeSources();

	TweakableClassLoader.IASMTransformer[] getASMTransformers();

}
