package net.openvoxel.api.mods;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.loader.classloader.TweakableClassLoader;

/**
 * Created by James on 25/08/2016.
 *
 * Interface Used By @ASMHandler Data
 */
@PublicAPI
public interface IASMHandler {

	TweakableClassLoader.IBytecodeSource[] getBytecodeSources();

	TweakableClassLoader.IASMTransformer[] getASMTransformers();

}
