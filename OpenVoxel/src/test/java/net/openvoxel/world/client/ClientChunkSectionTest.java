package net.openvoxel.world.client;

import net.openvoxel.common.block.BlockAir;
import net.openvoxel.common.registry.RegistryBlocks;
import net.openvoxel.common.util.BlockFace;
import net.openvoxel.vanilla.block.BlockBricks;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientChunkSectionTest {

	@DisplayName("Visibility Map Generation")
	@Test
	void generateVisibilityMap() {
		BlockBricks bricks = new BlockBricks();
		RegistryBlocks blockRegistry = new RegistryBlocks();
		blockRegistry.registerBlock("test:bricks",bricks);
		blockRegistry.generateMappingsFromRaw();

		ClientChunk _chunk = new ClientChunk(0,0);
		ClientChunkSection _section = _chunk.getSectionAt(0);

		//Filled with AIR {DEFAULT}
		_section.generateVisibilityMap(blockRegistry);
		for(int i = 0; i < 6; i++) {
			for(int j = 0; j < 6; j++) {
				assertTrue(_section.isVisible(i,j));
			}
		}

		//Fill with bricks
		for(int x = 0; x < 16;  x++) {
			for(int y = 0; y < 16; y++) {
				for(int z = 0; z < 16; z++) {
					_chunk.setBlock(blockRegistry,x,y,z,bricks,(byte)0);
					assertEquals(bricks,_section.blockAt(blockRegistry,x,y,z));
				}
			}
		}
		_section.generateVisibilityMap(blockRegistry);
		for(int i = 0; i < 6; i++) {
			for(int j = 0; j < 6; j++) {
				assertFalse(_section.isVisible(i,j));
			}
		}

		//Connect the top and the bottom
		for(int y = 0; y < 16; y++) {
			_chunk.setBlock(blockRegistry,8,y,8,BlockAir.BLOCK_AIR,(byte)0);
			assertEquals(BlockAir.BLOCK_AIR,_section.blockAt(blockRegistry,8,y,8));
		}
		for(int x = 0; x < 16; x++) {
			for(int y = 0; y < 16; y++) {
				for(int z = 0; z < 16; z++) {
					if(x == 8 && z == 8) {
						assertEquals(BlockAir.BLOCK_AIR,_section.blockAt(blockRegistry,x,y,z));
					}else{
						assertEquals(bricks,_section.blockAt(blockRegistry,x,y,z));
					}
				}
			}
		}
		_section.generateVisibilityMap(blockRegistry);
		for(BlockFace faceA : BlockFace.values()) {
			for(BlockFace faceB : BlockFace.values()) {
				String message = faceA.name() + " & " + faceB.name();
				if(faceA.yOffset != 0 && faceB.yOffset != 0) {
					assertTrue(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}else{
					assertFalse(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}
			}
		}

		//Connect X
		for(int x = 0; x < 16; x++) {
			_chunk.setBlock(blockRegistry,x,4,4,BlockAir.BLOCK_AIR,(byte)0);
			assertEquals(BlockAir.BLOCK_AIR,_section.blockAt(blockRegistry,x,4,4));
		}
		_section.generateVisibilityMap(blockRegistry);
		for(BlockFace faceA : BlockFace.values()) {
			for(BlockFace faceB : BlockFace.values()) {
				String message = faceA.name() + " & " + faceB.name();
				if(faceA.yOffset != 0 && faceB.yOffset != 0) {
					assertTrue(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}else if(faceA.xOffset != 0 && faceB.xOffset != 0) {
					assertTrue(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}else{
					assertFalse(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}
			}
		}


		//Clear
		for(int x = 0; x < 16;  x++) {
			for(int y = 0; y < 16; y++) {
				for(int z = 0; z < 16; z++) {
					_chunk.setBlock(blockRegistry,x,y,z,BlockAir.BLOCK_AIR,(byte)0);
				}
			}
		}
		//Add vertical wall
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				_chunk.setBlock(blockRegistry,x,8,z,bricks,(byte)0);
				assertEquals(bricks,_section.blockAt(blockRegistry,x,8,z));
			}
		}
		_section.generateVisibilityMap(blockRegistry);
		for(BlockFace faceA : BlockFace.values()) {
			for (BlockFace faceB : BlockFace.values()) {
				String message = faceA.name() + " & " + faceB.name();
				if(faceA.yOffset != 0 && faceB.yOffset != 0 && faceA != faceB) {
					assertFalse(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}else{
					assertTrue(_section.isVisible(faceA.faceID,faceB.faceID),message);
				}
			}
		}

		//Make Hole in center
		_chunk.setBlock(blockRegistry,8,8,8,BlockAir.BLOCK_AIR,(byte)0);
		assertEquals(BlockAir.BLOCK_AIR,_section.blockAt(blockRegistry,8,8,8));
		_section.generateVisibilityMap(blockRegistry);
		for(BlockFace faceA : BlockFace.values()) {
			for (BlockFace faceB : BlockFace.values()) {
				assertTrue(_section.isVisible(faceA.faceID,faceB.faceID));
			}
		}
	}
}