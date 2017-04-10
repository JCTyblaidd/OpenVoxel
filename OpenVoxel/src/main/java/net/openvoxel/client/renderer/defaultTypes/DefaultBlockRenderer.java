package net.openvoxel.client.renderer.defaultTypes;

import net.openvoxel.client.renderer.IBlockRenderHandler;
import net.openvoxel.client.renderer.generic.WorldRenderer;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.AABB;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 28/08/2016.
 *
 * Default Generic Block Renderer
 */
public class DefaultBlockRenderer implements IBlockRenderHandler{
	@Override
	public void storeBlockData(WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess) {
		Block block = stateAccess.getBlock();
		AABB bounds = block.getBlockBounds();
		if(block.isOpaque(BlockFace.UP) && stateAccess.getOffsetBlockData(BlockFace.UP).getBlock().isOpaque(BlockFace.DOWN)) {
			renderUp(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.DOWN) && stateAccess.getOffsetBlockData(BlockFace.DOWN).getBlock().isOpaque(BlockFace.UP)) {
			renderDown(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.WEST) && stateAccess.getOffsetBlockData(BlockFace.WEST).getBlock().isOpaque(BlockFace.EAST)) {
			renderWest(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.EAST) && stateAccess.getOffsetBlockData(BlockFace.EAST).getBlock().isOpaque(BlockFace.WEST)) {
			renderEast(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.NORTH) && stateAccess.getOffsetBlockData(BlockFace.NORTH).getBlock().isOpaque(BlockFace.SOUTH)) {
			renderNorth(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.SOUTH) && stateAccess.getOffsetBlockData(BlockFace.SOUTH).getBlock().isOpaque(BlockFace.NORTH)) {
			renderSouth(block,renderer,stateAccess,bounds);
		}
	}

	private void renderUp(Block block, WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.UP);
		renderer.setCurrentIcon(icon);
		float yVal = (float)bounds.maxY;
		float xmin = (float)bounds.minX;
		float xmax = (float)bounds.maxX;
		float zmin = (float)bounds.minZ;
		float zmax = (float)bounds.maxZ;
		renderer.addVertex(xmin,yVal,zmin,xmin,zmin,0,1,0);
		renderer.addVertex(xmin,yVal,zmax,xmin,zmax,0,1,0);
		renderer.addVertex(xmax,yVal,zmax,xmax,zmax,0,1,0);

		renderer.addVertex(xmin,yVal,zmin,xmin,zmin,0,1,0);
		renderer.addVertex(xmax,yVal,zmax,xmax,zmax,0,1,0);
		renderer.addVertex(xmax,yVal,zmin,xmax,zmin,0,1,0);
	}
	private void renderDown(Block block, WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.DOWN);
		renderer.setCurrentIcon(icon);
		float yVal = (float)bounds.minY;
		float xmin = (float)bounds.minX;
		float xmax = (float)bounds.maxX;
		float zmin = (float)bounds.minZ;
		float zmax = (float)bounds.maxZ;
		renderer.addVertex(xmax,yVal,zmax,xmax,zmax,0,-1,0);
		renderer.addVertex(xmin,yVal,zmax,xmin,zmax,0,-1,0);
		renderer.addVertex(xmin,yVal,zmin,xmin,zmin,0,-1,0);

		renderer.addVertex(xmax,yVal,zmin,xmax,zmin,0,-1,0);
		renderer.addVertex(xmax,yVal,zmax,xmax,zmax,0,-1,0);
		renderer.addVertex(xmin,yVal,zmin,xmin,zmin,0,-1,0);
	}
	private void renderWest(Block block, WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {

	}
	private void renderEast(Block block, WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {

	}
	private void renderNorth(Block block, WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {

	}
	private void renderSouth(Block block, WorldRenderer.WorldBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {

	}
}
