package net.openvoxel.client.renderer.common;

import org.jetbrains.annotations.NotNull;

/**
 * Graphics Quality Configuration
 *
 * TODO: Tex Atlas Size (after conversion to texture array array)
 *
 * TODO: PARTICLES
 */
public final class GraphicsConfig {

	public enum WorldDrawType {
		DRAW_FORWARD,
		DRAW_DEFERRED,
		DRAW_SHADOWED,
		DRAW_MAPPED,
		DRAW_VOXEL,
	}

	public enum AntiAliasing {
		MULTI_SAMPLE_8X,
		MULTI_SAMPLE_4X,
		MULTI_SAMPLE_2X,
		FAST_APPROXIMATE,
		NONE,
		//TODO: SubPixel AND/OR SuperSample??
	}

	public enum CloudType {
		VOLUMETRIC_CLOUDS,
		SIMPLE_CLOUDS,
		NO_CLOUDS
	}

	/**
	 * The Type of Rendering Path to Use
	 */
	@NotNull
	public WorldDrawType worldDrawType = WorldDrawType.DRAW_FORWARD;

	/**
	 * The Type of AntiAliasing To Use
	 */
	@NotNull
	public AntiAliasing antiAliasing = AntiAliasing.NONE;

	/**
	 * Number of shadow cascades to use
	 *  Ignored if drawType lacks shadows
	 *
	 * Valid Range: 1 <= cascade <= 4
	 */
	public int shadowCascadeCount = 1;

	/**
	 * The Size of the shadow maps to be used
	 *
	 *  Valid Range: 6 <= quality <= 12
	 *
	 *  Shadow Map Size = 1 << quality
	 */
	public int shadowQuality = 8;

	/*
	 * Enable/Disable Coloured Shadow Maps
	 */
	public boolean colouredShadows = false;

	/**
	 * The Size of the environmental / voxel map
	 *
	 *  Valid Range: TODO
	 */
	public int environmentQuality = 1;

	/**
	 * The Number of voxel cascades to use
	 *  Ignored if drawType lacks voxel mapping
	 *
	 *  Valid Range: 1 <= cascade <= 4
	 */
	public int voxelCascadeCount = 1;

	/**
	 * Choose type of clouds to be rendered
	 */
	@NotNull
	public CloudType qualityClouds = CloudType.NO_CLOUDS;

	/**
	 * Enable/Disable Screen Space Reflections
	 *  Ignored if a form of environmental mapping enabled
	 */
	public boolean screenSpaceReflections = false;

	/**
	 * Enable/Disable god rays
	 */
	public boolean enableGodRays = false;

	/**
	 * Enable/Disable fog
	 */
	public boolean enableFog = false;

	/**
	 * Enable/Disable Depth of Field
	 */
	public boolean enableDepthOfField = false;

	/**
	 * Enable/Disable High Dynamic Range
	 */
	public boolean enableHighDynamicRange = false;


	@SuppressWarnings("all")
	public boolean checkIfValid() {
		if(worldDrawType == null) return false;
		if(antiAliasing == null) return false;
		if(shadowCascadeCount < 1) return false;
		if(shadowCascadeCount > 4) return false;
		if(shadowQuality < 6) return false;
		if(shadowQuality > 12) return false;
		//TODO: ENVIRONMENTAL QUALITY
		if(voxelCascadeCount < 1) return false;
		if(voxelCascadeCount > 4) return false;
		if(qualityClouds == null) return false;
		return true;
	}

	//TODO: Serialize to/from JSON
}
