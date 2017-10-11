package net.openvoxel.client.renderer.vk.shader;

import net.openvoxel.utility.config.AutoConfig;
import net.openvoxel.utility.config.ConfigProperty;
import org.joml.Vector2f;

public class VkRenderConfig extends AutoConfig{

	public enum CullMethod {
		CULL_NONE,
		CULL_FRUSTRUM,
		CULL_SUBCHUNK,
	}

	public enum DisplayMode {
		DISPLAY_FULLSCREEN,
		DISPLAY_BORDERLESS_WINDOW,
		DISPLAY_WINDOWED,
	}

	@ConfigProperty("graphics_cull_method")
	public CullMethod cullMethod = CullMethod.CULL_SUBCHUNK;

	@ConfigProperty("graphics_clouds_enable")
	public boolean enableClouds = true;

	@ConfigProperty("graphics_gamma_value")
	public float chosenGamma = 1.0F / 2.2F;

	@ConfigProperty("graphics_shadow_mapping")
	public boolean enableShadowMapping = true;

	@ConfigProperty("graphics_cascade_shadows")
	public boolean enableCascadeShadowMaps = true;

	@ConfigProperty("graphics_resolution")
	public Vector2f chosenResolution;

	@ConfigProperty("graphics_vk_mailbox")
	public boolean useMailboxPresent = false;

	@ConfigProperty("graphics_framerate_limiter")
	public int frameRateLimiter = Integer.MAX_VALUE;

	@ConfigProperty("graphics_display_mode")
	public DisplayMode displayMode = DisplayMode.DISPLAY_FULLSCREEN;


	@Override
	protected String getFileName() {
		return "graphics_config.cfg";
	}
}
