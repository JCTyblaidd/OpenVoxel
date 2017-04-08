package net.openvoxel.client.renderer.generic.config;

/**
 * Created by James on 11/09/2016.
 *
 * Config for Render Pipeline
 */
public class RenderConfig {

///////////////SECTION: general display///////////////////////////////////////

	/**
	 * Targeted FPS Count:
	 */
	public int targetFPS = 60;

	/**
	 * Use V-Sync
	 */
	public boolean enableVsync = true;

	/**
	 * Level of Compression for Texture Atlas
	 */
	public CompressionLevel textureAtlasCompression = CompressionLevel.NO_COMPRESSION;

	/**
	 * Size of Texture Atlas
	 */
	public int pow2AtlasSize = 64;//Valid = Powers of 2//

	/**
	 * Use Fullscreen Mode
	 */
	public boolean fullScreen = false;

	/**
	 * OpenGL: Allow use of extensions and OpenGL > 3.3
	 * Vulkan: ???
	 */
	public boolean allowAdvancedOptimizations = true;

	/**
	 * Number of Chunks To Draw
	 */
	public int drawDistance = 32;

////////////////SECTION: world rendering///////////////////////////////////

	public boolean useDeferredPipeline = false;//TODO: change back to true

/////////////////SECTION: deferred rendering//////////////////////////////////

	public boolean EnableParallax = true;

	public CloudQuality cloudQuality = CloudQuality.VOLUMETRIC_CLOUDS;

	public boolean cloudShadows = true;

	public boolean EnableFog = true;

	public boolean EnableGodRays = true;

	/**
	 * Use Normal Calculation Mechanism that works for non axis directions
	 */
	public boolean AccurateNormalCalculation = true;

	public boolean EnableAntiAliasing = true;

	public boolean EnableBonusReflectionArea = true;

	public boolean FastTransparency = false;

	public boolean HighQualityLightEmission = true;

	public ShadowQuality shadowQuality = ShadowQuality.CASCADE_SHADOW_MAP;

	public boolean EnableReflections = true;

	public boolean EnableDepthOfField = true;

//////////SECTION: generic world ///////////////////////////////

	public boolean EnableWindAnimation = true;

	public boolean EnableWaveAnimaion = true;

}
