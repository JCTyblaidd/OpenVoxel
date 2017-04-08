package net.openvoxel.client.renderer.gl3;

import net.openvoxel.client.renderer.gl3.util.OGL3FrameBufferObject;

/**
 * Created by James on 28/08/2016.
 *
 * Utility : GBuffer Renderer
 */
public class OGL3GBufferManager {

	private OGL3FrameBufferObject f;

	public void initialize() {

	}

	public void checkAndHandleResize() {

	}

	/*
		Outputs[reset]:
			depth,
			color1=UV_coord(rg),
			color2=Normal_Main(rgb)
			color3=Color_Mask(rgb)
			color4=Light_Data(rgba)(a=skylight)
			if(transparent) Separate Buffers
			if(transparent & !transparencyDetail) {
				depth,
				color1 = Color(rgba) (NO MORE INFORMATION)
			}
	 */
	public void prepareForPass_StoreBlockData1(boolean isOpaque,boolean transparencyDetail) {

	}

	/*
		Cascade Map[x3 Max, may be less]:
		Outputs[reset]:
			depth
	 */
	public void prepareForPass_CreateShadowMap(int Cascade) {

	}

	/*
		Outputs[reset]:
			depth,
			color1=color(rgb)
	 */
	public void prepareForPass_BackwardsDiffuse() {

	}

	/*
		Outputs[reset]:
			depth,
			color1=diffuse
			color2=light_data
	 */
	public void prepareForPass_StoreEntityData() {

	}
	/*
		Outputs[reset]:
			color1=diffuse
			color2=realNormal
			color3=pbrData
			color4[noreset from input]=previous_light_data
	 */
	public void prepareForPass_GeneratePBRBlockData(boolean isOpaque,boolean transparencyDetail) {

	}

	/*
		Outputs[reset]:
			if(!post) mainOut
			else post(rgb)
	 */
	public void prepareForPass_FinalResolve(boolean postprocessing_enabled) {

	}

	/*
		Outputs[already reset]:
			mainOut
	 */
	public void prepareForPass_PostProcessing() {

	}

}
