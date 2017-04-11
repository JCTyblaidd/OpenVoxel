package net.openvoxel.client.renderer.gl3.worldrender.deferred_path;

import net.openvoxel.world.client.ClientChunkSection;
import org.joml.Matrix4f;

import java.util.List;

import static net.openvoxel.client.renderer.gl3.OGL3WorldRenderer.Z_FAR;

/**
 * Created by James on 11/04/2017.
 *
 * Manages Calculating of Shadow Map Cascades
 */
public class OGL3CascadeManager {

	/*
	 * Hard Coded Constant
	 */
	public static final int NumCascades = 3;

	/**
	 * Shadow Map Cascade Splitting
	 */
	public static final float[] CASCADE_SPLITS = new float[]{Z_FAR / 20.0f, Z_FAR / 10.0f, Z_FAR};

	/**
	 * World -> Screen Matrices
	 */
	private static Matrix4f[] CASCADE_MATRICES = new Matrix4f[]{new Matrix4f(), new Matrix4f(), new Matrix4f()};
	/**
	 * Shadow 2D Array OpenGL Handle
	 */
	private int shadowCascadeArray;

	/**
	 * Shadow Rendering Draw Target
	 */
	private int shadowRenderBuffer;

	/**
	 * Calculate the World -> Screen Matrix Values
	 */
	private void generateWorldToScreenMatrices() {

	}

	/**
	 * Calculate And Update Shadow Uniform Information
	 */
	void updateShadowInfoUniform() {

	}

	public void renderShadowCascades(List<ClientChunkSection> toRender) {

	}


}
