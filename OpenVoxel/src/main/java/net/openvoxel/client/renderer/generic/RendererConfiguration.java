package net.openvoxel.client.renderer.generic;

import com.jc.util.format.json.JSONMap;
import com.jc.util.format.json.JSONObject;
import net.openvoxel.api.util.Quality;

/**
 * Created by James on 25/08/2016.
 *
 * Planned Maximum Rendering Values:    Global Illumination w/ Volumetric Lighting And Screen-Space Reflection
 *                                      Physically Based Rendering
 *                                      Soft Shadows, Extended w/ Cascade Map For Sun
 *                                      Additional PBR Values, for Liquids ETC. + Parralax Occulsion Mapping
 *                                      Realistic Weather + Coloured Lighting + Volumetric Clouds
 *  GBuffer Plan:
 *
 */
@Deprecated
public class RendererConfiguration {

	/**
	 * Volumetric Clouds: Enable / Disable
	 */
	public boolean Volumetric_Clouds;
	/**
	 * Cloud Shadows: Enable / Disable
	 */
	public boolean Cloud_Shadows;
	/**
	 * Draw Clouds: Enable / Disable
	 */
	public boolean Draw_Clouds;

	/**
	 * Distance Fod: Enable / Disable
	 */
	public boolean Enable_Fog;
	/**
	 * Parallax Mapping: Enable / Disable
	 */
	public boolean Enable_Parallax;

	/**
	 * Volumetric Light From The Sun: Enable / Disable
	 */
	public boolean Enable_Volumetric_Lighting;

	/**
	 * Shadow Quality:
	 *  None: No Shadows
	 *  Low -> High: Increase In Quality And Cascade Sections
	 */
	public Quality Shadow_Quality;

	/**
	 * Wave Animation: Enable / Disable
	 */
	public boolean Enable_Wave_Animation;

	/**
	 * Wind Animation: Enable / Disable
	 */
	public boolean Enable_Wind_Animation;

	/**
	 * Enable: Use Normal Calculation that works for normals that arn't in the voxel plane(breaks excessively otherwise but in rare cases that might not even exist)
	 */
	public boolean Use_Accurate_Normal_Calculation;

	/**
	 * AntiAliasing: Enable / Disable
	 */
	public boolean Enable_AntiAliasing;

	/**
	 * Reflections Behind Your via a seperate diffuse sample (quite a bit of performance for a tiny gain)
	 */
	public boolean Enable_Extended_Reflection_Area;

	/**
	 * Use Screen Space Reflections: Disable, than difference in sampling area//
	 */
	public Quality Reflection_Quality;

	/**
	 * Use Additional Light Emission: Enable / Disable
	 */
	public boolean Enable_Light_Emission;

	/**
	 * Use PBR on Transparent Objects: Enable / Disable -> (saves memory + some processing)
	 */
	public boolean High_Quality_Transparency;

	/**
	 * The Amount Of Chunks To Draw
	 */
	public int Draw_Distance;

	public JSONObject toJSON() {
		JSONMap dat = new JSONMap();
		dat.put("Volumetric Clouds",Volumetric_Clouds);
		dat.put("Cloud Shadows",Cloud_Shadows);
		dat.put("Draw Clouds",Draw_Clouds);
		dat.put("Enable Fog",Enable_Fog);
		dat.put("Enable Parallax",Enable_Parallax);
		dat.put("Enable Volumetric Lighting",Enable_Volumetric_Lighting);
		dat.put("Shadow Quality",Shadow_Quality.name());
		dat.put("Enable Wave Animation",Enable_Wave_Animation);
		dat.put("Enable Wind Animation",Enable_Wind_Animation);
		dat.put("Use Accurate Normal Calculation",Use_Accurate_Normal_Calculation);
		dat.put("Enable AntiAliasing",Enable_AntiAliasing);
		dat.put("Enable Extended Reflection",Enable_Extended_Reflection_Area);
		dat.put("Reflection Quality",Reflection_Quality.name());
		dat.put("Enable Light Emission",Enable_Light_Emission);
		dat.put("High Quality Transparency",High_Quality_Transparency);
		dat.put("Draw Distance",Draw_Distance);
		return dat;
	}

	public void fromJSON(JSONObject json) {
		JSONMap map = json.asMap();
		Volumetric_Clouds = map.get("Volumetric Clouds").asBool();
		Cloud_Shadows = map.get("Cloud Shadows").asBool();
		Draw_Clouds = map.get("Draw Clouds").asBool();
		Enable_Fog = map.get("Enable Fog").asBool();
		Enable_Parallax = map.get("Enable Parallax").asBool();
		Enable_Volumetric_Lighting = map.get("Enable Volumetric Lighting").asBool();
		Shadow_Quality = Quality.valueOf(map.get("Shadow Quality").asString());
		Enable_Wave_Animation = map.get("Enable Wave Animation").asBool();
		Enable_Wind_Animation = map.get("Enable Wind Animation").asBool();
		Use_Accurate_Normal_Calculation = map.get("Use Accurate Normal Calculation").asBool();
		Enable_AntiAliasing = map.get("Enable AntiAliasing").asBool();
		Enable_Extended_Reflection_Area = map.get("Enable Extended Reflection").asBool();
		Reflection_Quality = Quality.valueOf(map.get("Reflection Quality").asString());
		Enable_Light_Emission = map.get("Enable Light Emission").asBool();
		High_Quality_Transparency = map.get("High Quality Transparency").asBool();
		Draw_Distance = map.get("Draw Distance").asInteger();
	}

	public void setDefaults_Ultra() {
		//Everything Enabled//
		Volumetric_Clouds = true;
		Cloud_Shadows = true;
		Draw_Clouds = true;
		Enable_Fog = true;
		Enable_Parallax = true;
		Enable_Volumetric_Lighting = true;
		Shadow_Quality = Quality.ULTRA;
		Enable_Wave_Animation = true;
		Enable_Wind_Animation = true;
		Use_Accurate_Normal_Calculation = true;
		Enable_AntiAliasing = true;
		Enable_Extended_Reflection_Area = true;
		Reflection_Quality = Quality.ULTRA;
		Enable_Light_Emission = true;
		High_Quality_Transparency = true;
		Draw_Distance = 32;
	}
	public void setDefaults_High() {
		Volumetric_Clouds = true;
		Cloud_Shadows = true;
		Draw_Clouds = true;
		Enable_Fog = true;
		Enable_Parallax = true;
		Enable_Volumetric_Lighting = true;
		Shadow_Quality = Quality.HIGH;//Reduce Shadow Quality//
		Enable_Wave_Animation = true;
		Enable_Wind_Animation = true;
		Use_Accurate_Normal_Calculation = true;
		Enable_AntiAliasing = true;
		Enable_Extended_Reflection_Area = false;//Disable: Lots of cost, tiny gain
		Reflection_Quality = Quality.HIGH;//Reduce Quality//
		Enable_Light_Emission = true;
		High_Quality_Transparency = false;//Reduce Memory and Save some processing
		Draw_Distance = 24;
	}
	public void setDefaults_Standard() {
		Volumetric_Clouds = true;
		Cloud_Shadows = false;//Tiny Improvement//
		Draw_Clouds = true;
		Enable_Fog = true;
		Enable_Parallax = true;
		Enable_Volumetric_Lighting = false;//Save Some Performance//
		Shadow_Quality = Quality.NORMAL;//Reduce Shadow Quality//
		Enable_Wave_Animation = true;
		Enable_Wind_Animation = true;
		Use_Accurate_Normal_Calculation = true;
		Enable_AntiAliasing = true;
		Enable_Extended_Reflection_Area = false;
		Reflection_Quality = Quality.NORMAL;//Reduce Quality//
		Enable_Light_Emission = true;
		High_Quality_Transparency = false;
		Draw_Distance = 16;
	}
	public void setDefaults_Low() {
		Volumetric_Clouds = false;//Save Some Performance
		Cloud_Shadows = false;
		Draw_Clouds = true;
		Enable_Fog = true;
		Enable_Parallax = true;//Keep - Shouldn't Be That Bad
		Enable_Volumetric_Lighting = false;
		Shadow_Quality = Quality.LOWEST;//ReduceQuality//
		Enable_Wave_Animation = false;//Save Some Performance With The Animation
		Enable_Wind_Animation = false;
		Use_Accurate_Normal_Calculation = true;
		Enable_AntiAliasing = true;
		Enable_Extended_Reflection_Area = false;
		Reflection_Quality = Quality.NONE;//Reduce Quality//
		Enable_Light_Emission = true;
		Draw_Distance = 12;
	}
	public void setDefaults_Lowest() {
		//Most Disabled//
		Volumetric_Clouds = false;
		Cloud_Shadows = false;
		Draw_Clouds = true;
		Enable_Fog = true;
		Enable_Parallax = false;
		Enable_Volumetric_Lighting = false;
		Shadow_Quality = Quality.NONE;
		Enable_Wave_Animation = false;
		Enable_Wind_Animation = false;
		Use_Accurate_Normal_Calculation = true;
		Enable_AntiAliasing = true;
		Enable_Extended_Reflection_Area = false;
		Reflection_Quality = Quality.NONE;
		Enable_Light_Emission = false;
		Draw_Distance = 8;
	}
}
