package net.openvoxel.common.block;

import net.openvoxel.api.side.Side;
import net.openvoxel.api.side.SideOnly;
import net.openvoxel.client.renderer.common.DefaultBlockRenderer;
import net.openvoxel.client.renderer.common.IBlockRenderHandler;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.util.AABB;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 25/08/2016.
 */
public abstract class Block {

	public static AABB SOLID_BLOCK_AABB = new AABB(0,0,0,1,1,1);
	public static AABB EMPTY_BLOCK_AABB = new AABB(0,0,0,0,0,0);

	public static IBlockRenderHandler defaultRenderHandler = new DefaultBlockRenderer();
	public static IBlockRenderHandler emptyRenderHandler = (renderer, stateAccess,isOpaque) -> {};//Draw Nothing//

	protected float explosion_resistance = 1.0F;
	protected int light_emission = 0;

	protected float friction;//0->1, 0=noFriction, 1=Normal Walking, >1 = Slow Walking

	public float getExplosionResistance(BlockFace face) {
		return explosion_resistance;
	}

	public int getLightEmitted(BlockFace face) {
		return light_emission;
	}

	public float getFriction() {
		return friction;
	}

	/**
	 * @return The Resources That Are Required For Stitching
	 */
	@SideOnly(side = Side.CLIENT)
	public abstract void loadTextureAtlasData(IconAtlas texAtlas);

	/**
	 * Used By Default Renderer: return the Icon For Outputting
	 * @param blockAccess the block reference
	 * @param face the face being rendered
	 * @return the icon[null for no render]
	 */
	@SideOnly(side = Side.CLIENT)
	public abstract Icon getIconAtSide(IBlockAccess blockAccess, BlockFace face);

	/**
	 * @return if this blocks chunk should be re-rendered if a block
	 * next to it on the next chunk is re-rendered
	 */
	@SideOnly(side = Side.CLIENT)
	public boolean doesBlockReRenderOnNearbyUpdate() {
		return false;
	}

	//TODO: CHANGE isOpaque & isCompleteOpaque and other to accept metadata!!!

	//Client Side Rendering State
	public boolean isOpaque(BlockFace face) {
		return true;
	}

	//Is Drawn in opaque draw or transparent draw
	public boolean isCompleteOpaque() {
		return true;
	}

	//Use to optimize + check if the block should be updated
	public boolean hasRandomBlockUpdates() {
		return false;
	}

	// TODO: 25/08/2016 BlockState Ref 
	public void onRandomBlockUpdate(IBlockAccess blockAccess) {
		//NADA:
	}

	public Class<?> getTileEntityClass() {
		return null;
	}
	public boolean hasTileEntity() {
		return false;
	}

	public AABB getBlockBounds() {
		return SOLID_BLOCK_AABB;
	}

	public IBlockRenderHandler getRenderHandler() {
		return defaultRenderHandler;
	}

}
