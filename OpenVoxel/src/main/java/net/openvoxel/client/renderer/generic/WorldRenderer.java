package net.openvoxel.client.renderer.generic;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.entity.living.player.EntityPlayerSP;
import net.openvoxel.world.client.ClientChunk;
import net.openvoxel.world.client.ClientWorld;

/**
 * Created by James on 25/08/2016.
 *
 * World Render Process Consideration #1:
 *
 * Draw_Progression(same basic implementation across differing renderer types - just vk is more multithreaded):
 *  1. Write Block UV,Col,Normal,Light Data to gBuffer: Opaque + Transparent                                        Draw: visible Chunks
 *  2. If Enabled: Create Cascade Shadow Map                                                                        ReDraw: visible Chunks
 *  3. Write Entity Data to Diffuse + Light Map For Entities //{no lightmap? currVer}                               Draw: visible Entities In Chunks
 *  4. If Enabled: Create Behind Player Diffuse Map                                                                 Draw: non-visible Chunks
 *  5. Convert UV Data to PBR Data w/ Col spliced with diffuse (parallax etc.): Opaque + Transparent                Processing
 *  6. Run Final Resolve w/ Lighting Effects                                                                        Processing
 *  7. If Enabled: Run PostProcessing                                                                               Processing
 *
 *  8. GUI Rendered After - On Top Of Information                                                                   GUI
 *
 *  Buffers For Final Resolve:
 *      [
 *      Diffuse_Opaque, Diffuse_Transparent,
 *      Depth_Opaque, Depth_Transparent,
 *      Normal_Opaque, Normal_Transparent,
 *      PBR_Opaque, PBR_Transparent,
 *      Light_Opaque, Light_Transparent,
 *      Entity_Diffuse, Entity_Lighting,
 *      Cascade_Depth[3],
 *      Behind_Diffuse
 *      ]x16 buffers
 *      (transparent only gets diffuse if high quality transparency is disabled)
 *
 *
 *
 *  World Render Process Consideration #2[Option = Deferred]:
 *      1. Write Diffuse,Normal,Light,PBR Data to GBuffer (MRT) (Opaque Only): Apply: Parallax, and special vertex transforms
 *      2. Write Diffuse,PBR,Normal to GBuffer (MRT+) (Transparency: ) (if possible: use same pass as 1. w/ tex2darray)
 *      3. Create Cascade Shadow Map (geometry shader trick if applicable on hardware)(block + entity)
 *      4. Depth Culled: Draw Entities: Diffuse + LightMap
 *      5. Run Final Pass #1
 *      6. Run PostProcessing Pass
 *
 *  World Renderer Process Consideration #2[Option = Forward]:
 *      1. Draw Block Data => Colour (no effects):
 *      2. Draw Entity Data => Colour (no effects):
 *      3.[?] Post Processing Pass
 *
 *
 *  World Renderer Process Consideration #3[] {pre=cull + gen uniforms}:
 *      0. For All Pixels: Generate Final Resolve Image //#Convert to PostProcess w/ depth check
 *      1. For All Visible Chunks + Entities(post) => Iterate from near to far => Output(Diffuse,Normal,Lighting,PBR,Depth)(Opaque Only)
 *          >> Apply: Parallax + Calc Real Normals
 *      2. For All Relevant Chunks + Entities(post) => Iterate from near sun to far => Output(x3 via GEOM + 2DTex?) => Cascade Shadow Map
 *      3. For All Visible Chunks => Iterate from near to far => Output(Diffuse Only)(Transparent Only)(Depth test against opaque)
 *          >> Using Approximate Transparency + shadow_map?
 *      3.5?: If Enabled => Generate Behind Player Diffuse Map (Diffuse + Depth):
 *      5. Finalize Pass #1: Calculate Opaque Final Diffuse From Lighting Value
 *      4. Finalize Pass #2: Resolve HDR Lighting Convert To Glow Map
 *      6. Finalize Pass #3: Apply Effects Then Blend Inversely Transparency Value
 *          >> Effects: SSR, (SSAO?), GodRays, Glow Data
 *      6. Post Processing #1 Anti-Alias Pass => Final Framebuffer
 *
 */
public interface WorldRenderer {

	//Handle Dirty Chunks In This//
	void renderWorld(EntityPlayerSP playerSP,ClientWorld worldSP);

	///
	/// Handling Chunk Updates : Unloads are handled via the release data method
	///
	void onChunkLoaded(ClientChunk chunk);
	void onChunkDirty(ClientChunk chunk);
	void onChunkUnloaded(ClientChunk chunk);

	/**
	 * Interface for the generation of block data from the world space
	 * the len(norm values) MUST equal 1
	 * the UV values MUST reference the texture atlas locations
	 * the XYZ values are in the scaling of 0->1 (all offsets are handled by the renderer)
	 * Light Values Are Automatically Calculated From Requested Draw Data Position And Block Light Value
	 */
	interface WorldBlockRenderer {
		/**
		 * Normal add Vertex Draw Request
		 */
		void addVertex(float X, float Y, float Z, float U, float V,float xNorm,float yNorm,float zNorm,float xTangent,float yTangent, float zTangent);

		/**
		 * Add Vertex Draw Request For Block Sections With Variable Colours
		 */
		void addVertexWithCol(float X, float Y, float Z, float U, float V,float xNorm,float yNorm,float zNorm,float xTangent,float yTangent, float zTangent, int Colour);

		/**
		 * Add Vertex Draw Request For Block Sections With Variable Colours & Flags
		 * Valid Flags:
		 *  TODO: IMPLEMENT
		 */
		void addVertexWithColFlags(float X, float Y, float Z, float U, float V, float xNorm, float yNorm, float zNorm, float xTangent, float yTangent, float zTangent, int Colour,int flags);

		/**
		 * notifyEvent that UV: values are to be considered in relation to this icon
		 * @param icon
		 */
		void setCurrentIcon(Icon icon);
	}
}
