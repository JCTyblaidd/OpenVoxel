package net.openvoxel.client.renderer.common;

import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.common.block.Block;
import net.openvoxel.common.block.IBlockAccess;
import net.openvoxel.common.util.AABB;
import net.openvoxel.common.util.BlockFace;

/**
 * Created by James on 28/08/2016.
 *
 * Default Generic Block Renderer
 *
 * TODO: check the uv coordinates and the tangent and bi tangent values
 */
public class DefaultBlockRenderer implements IBlockRenderHandler {


	@Override
	public void storeBlockData(IBlockRenderer renderer, IBlockAccess stateAccess, boolean opaqueDraw) {
		Block block = stateAccess.getBlock();
		//Skip Draw if not applicable//
		if(block.isCompleteOpaque() != opaqueDraw) return;
		AABB bounds = block.getBlockBounds();
		if(block.isOpaque(BlockFace.UP) && !stateAccess.getOffsetBlockData(BlockFace.UP).getBlock().isOpaque(BlockFace.DOWN)) {
			renderUp(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.DOWN) && !stateAccess.getOffsetBlockData(BlockFace.DOWN).getBlock().isOpaque(BlockFace.UP)) {
			renderDown(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.WEST) && !stateAccess.getOffsetBlockData(BlockFace.WEST).getBlock().isOpaque(BlockFace.EAST)) {
			renderWest(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.EAST) && !stateAccess.getOffsetBlockData(BlockFace.EAST).getBlock().isOpaque(BlockFace.WEST)) {
			renderEast(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.NORTH) && !stateAccess.getOffsetBlockData(BlockFace.NORTH).getBlock().isOpaque(BlockFace.SOUTH)) {
			renderNorth(block,renderer,stateAccess,bounds);
		}
		if(block.isOpaque(BlockFace.SOUTH) && !stateAccess.getOffsetBlockData(BlockFace.SOUTH).getBlock().isOpaque(BlockFace.NORTH)) {
			renderSouth(block,renderer,stateAccess,bounds);
		}
	}

	private void renderUp(Block block, IBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.UP);
		renderer.setCurrentIcon(icon);
		float yVal = (float)bounds.maxY;
		float xMin = (float)bounds.minX;
		float xMax = (float)bounds.maxX;
		float zMin = (float)bounds.minZ;
		float zMax = (float)bounds.maxZ;
		renderer.addVertex(xMax,yVal,zMax,xMax,zMax,0,1,0,1,0,0);
		renderer.addVertex(xMin,yVal,zMax,xMin,zMax,0,1,0,1,0,0);
		renderer.addVertex(xMin,yVal,zMin,xMin,zMin,0,1,0,1,0,0);

		renderer.addVertex(xMax,yVal,zMin,xMax,zMin,0,1,0,1,0,0);
		renderer.addVertex(xMax,yVal,zMax,xMax,zMax,0,1,0,1,0,0);
		renderer.addVertex(xMin,yVal,zMin,xMin,zMin,0,1,0,1,0,0);
	}
	private void renderDown(Block block, IBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.DOWN);
		renderer.setCurrentIcon(icon);
		float yVal = (float)bounds.minY;
		float xMin = (float)bounds.minX;
		float xMax = (float)bounds.maxX;
		float zMin = (float)bounds.minZ;
		float zMax = (float)bounds.maxZ;
		renderer.addVertex(xMin,yVal,zMin,xMin,zMin,0,-1,0,-1,0,0);
		renderer.addVertex(xMin,yVal,zMax,xMin,zMax,0,-1,0,-1,0,0);
		renderer.addVertex(xMax,yVal,zMax,xMax,zMax,0,-1,0,-1,0,0);

		renderer.addVertex(xMin,yVal,zMin,xMin,zMin,0,-1,0,-1,0,0);
		renderer.addVertex(xMax,yVal,zMax,xMax,zMax,0,-1,0,-1,0,0);
		renderer.addVertex(xMax,yVal,zMin,xMax,zMin,0,-1,0,-1,0,0);
	}
	private void renderWest(Block block, IBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.WEST);
		renderer.setCurrentIcon(icon);
		float zVal = (float)bounds.maxZ;
		float xMin = (float)bounds.minX;
		float xMax = (float)bounds.maxX;
		float yMin = (float)bounds.minY;
		float yMax = (float)bounds.maxY;
		renderer.addVertex(xMin,yMin,zVal,xMin,yMin,0,0,1,1,0,0);
		renderer.addVertex(xMin,yMax,zVal,xMin,yMax,0,0,1,1,0,0);
		renderer.addVertex(xMax,yMax,zVal,xMax,yMax,0,0,1,1,0,0);

		renderer.addVertex(xMin,yMin,zVal,xMin,yMin,0,0,1,1,0,0);
		renderer.addVertex(xMax,yMax,zVal,xMax,yMax,0,0,1,1,0,0);
		renderer.addVertex(xMax,yMin,zVal,xMax,yMin,0,0,1,1,0,0);
	}
	private void renderEast(Block block, IBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.EAST);
		renderer.setCurrentIcon(icon);
		float zVal = (float)bounds.minZ;
		float xMin = (float)bounds.minX;
		float xMax = (float)bounds.maxX;
		float yMin = (float)bounds.minY;
		float yMax = (float)bounds.maxY;
		renderer.addVertex(xMax,yMax,zVal,xMax,yMax,0,0,-1,-1,0,0);
		renderer.addVertex(xMin,yMax,zVal,xMin,yMax,0,0,-1,-1,0,0);
		renderer.addVertex(xMin,yMin,zVal,xMin,yMin,0,0,-1,-1,0,0);

		renderer.addVertex(xMax,yMin,zVal,xMax,yMin,0,0,-1,-1,0,0);
		renderer.addVertex(xMax,yMax,zVal,xMax,yMax,0,0,-1,-1,0,0);
		renderer.addVertex(xMin,yMin,zVal,xMin,yMin,0,0,-1,-1,0,0);
	}
	private void renderNorth(Block block, IBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess, BlockFace.NORTH);
		renderer.setCurrentIcon(icon);
		float xVal = (float)bounds.maxX;
		float yMin = (float)bounds.minY;
		float yMax = (float)bounds.maxY;
		float zMin = (float)bounds.minZ;
		float zMax = (float)bounds.maxZ;
		renderer.addVertex(xVal,yMin,zMin,yMin,zMin,1,0,0,0,1,0);
		renderer.addVertex(xVal,yMin,zMax,yMax,zMin,1,0,0,0,1,0);
		renderer.addVertex(xVal,yMax,zMax,yMax,zMax,1,0,0,0,1,0);

		renderer.addVertex(xVal,yMin,zMin,yMin,zMin,1,0,0,0,1,0);
		renderer.addVertex(xVal,yMax,zMax,yMax,zMax,1,0,0,0,1,0);
		renderer.addVertex(xVal,yMax,zMin,yMin,zMax,1,0,0,0,1,0);
	}
	private void renderSouth(Block block, IBlockRenderer renderer, IBlockAccess stateAccess,AABB bounds) {
		Icon icon = block.getIconAtSide(stateAccess,BlockFace.SOUTH);
		renderer.setCurrentIcon(icon);
		float xVal = (float)bounds.minX;
		float yMin = (float)bounds.minY;
		float yMax = (float)bounds.maxY;
		float zMin = (float)bounds.minZ;
		float zMax = (float)bounds.maxZ;
		renderer.addVertex(xVal,yMax,zMax,yMax,zMax,-1,0,0,0,-1,0);
		renderer.addVertex(xVal,yMin,zMax,yMax,zMin,-1,0,0,0,-1,0);
		renderer.addVertex(xVal,yMin,zMin,yMin,zMin,-1,0,0,0,-1,0);

		renderer.addVertex(xVal,yMax,zMin,yMin,zMax,-1,0,0,0,-1,0);
		renderer.addVertex(xVal,yMax,zMax,yMax,zMax,-1,0,0,0,-1,0);
		renderer.addVertex(xVal,yMin,zMin,yMin,zMin,-1,0,0,0,-1,0);
	}
}
