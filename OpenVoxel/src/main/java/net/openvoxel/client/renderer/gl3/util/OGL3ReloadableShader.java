package net.openvoxel.client.renderer.gl3.util;

import net.openvoxel.common.resources.ResourceHandle;

/**
 * Created by James on 25/08/2016.
 */
public abstract class OGL3ReloadableShader<T extends OGL3BasicShader> {

	protected ResourceHandle handle;
	protected T shader;
	public OGL3ReloadableShader(ResourceHandle h) {
		handle = h;
	}

	public T getShader() {
		if(handle.checkIfChanged()) {
			if(shader != null) {
				shader.deleteProgram();//Cleanup//
			}
			shader = newShader(handle.getStringData());
		}else if(shader == null) {
			shader = newShader(handle.getStringData());
		}
		return shader;
	}

	public void use() {
		getShader().Use();
	}
	public void forget() {
		getShader().Forget();
	}

	public abstract T newShader(String src);

}
