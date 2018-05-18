package net.openvoxel.client.textureatlas;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.STBITexture;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.files.util.FolderUtils;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.MathUtilities;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.stb.STBImageResize.*;
import static org.lwjgl.stb.STBRectPack.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class BaseAtlas implements IconAtlas {

	private boolean isSingleTexture;
	private List<BaseIcon> iconList;
	private Map<BaseIcon,ResourceHandle> refIconDiff;
	private Map<BaseIcon,ResourceHandle> refIconNorm;
	private Map<BaseIcon,ResourceHandle> refIconPBR;

	public int AtlasWidth;
	public int AtlasHeight;
	public int AtlasMipLevels;
	public ByteBuffer DataDiff;
	public ByteBuffer DataNorm;
	public ByteBuffer DataPBR;

	public BaseAtlas(boolean isSingleTexture) {
		this.isSingleTexture = isSingleTexture;
		refIconDiff = new HashMap<>();
		iconList = new ArrayList<>();
		if(!isSingleTexture) {
			refIconNorm = new HashMap<>();
			refIconPBR = new HashMap<>();
		}
	}

	public BaseIcon registerSingle(ResourceHandle handle) {
		BaseIcon ref = new BaseIcon();
		iconList.add(ref);
		refIconDiff.put(ref,handle);
		return ref;
	}

	public BaseIcon register(ResourceHandle diff,ResourceHandle norm,ResourceHandle pbr) {
		BaseIcon ref = new BaseIcon();
		iconList.add(ref);
		refIconDiff.put(ref,diff);
		refIconNorm.put(ref,norm);
		refIconPBR.put(ref,pbr);
		return ref;
	}

	@Override
	public void performStitch() {
		stitchAndGenerateAtlas("block");
	}


/*	private void resize(long ptr_input,int old_mip_size,long ptr_output,int mip_size) {
		/*STBImageResize.nstbir_resize_uint8(
				ptr_input, old_mip_size, old_mip_size, 0,
				ptr_output, mip_size, mip_size, 0,
				4
		);*/
		/*nstbir_resize_uint8_generic(
				ptr_input, old_mip_size, old_mip_size, 0,
				ptr_output, mip_size, mip_size, 0,
				4,
				STBIR_ALPHA_CHANNEL_NONE,
				0,
				STBIR_EDGE_CLAMP,
				STBIR_FILTER_TRIANGLE,   //CHANGED FROM DEFAULT
				STBIR_COLORSPACE_LINEAR,
				0L
		);
	}*/
	private void resize(ByteBuffer buffer, int offset_input, int old_mip_size, int offset_output, int mip_size) {
		long ptr_input = MemoryUtil.memAddress(buffer,offset_input);
		long ptr_output = MemoryUtil.memAddress(buffer,offset_output);
		STBImageResize.nstbir_resize_uint8(
				ptr_input, old_mip_size, old_mip_size, 0,
				ptr_output, mip_size, mip_size, 0,
				4
		);
		/*
		for(int x = 0; x < mip_size; x++) {
			for(int y = 0; y < mip_size; y++) {
				int offset_out = offset_output + 4 * (x + y * mip_size);
				int offset_in1 = offset_input  + 8 * (x + y * old_mip_size);
				int offset_in2 = offset_in1 + 4;
				int offset_in3 = offset_in1 + 4 * old_mip_size;
				int offset_in4 = offset_in3 + 4;
				for(int byte_off = 0; byte_off < 4; byte_off++) {
					int VAL1 = buffer.get(offset_in1+byte_off) & 0xFF;
					int VAL2 = buffer.get(offset_in2+byte_off) & 0xFF;
					int VAL3 = buffer.get(offset_in3+byte_off) & 0xFF;
					int VAL4 = buffer.get(offset_in4+byte_off) & 0xFF;
					int RES = (VAL1 + VAL2 + VAL3 + VAL4 + 2) / 4;//ROUNDED
					buffer.put(offset_out+byte_off,(byte)RES);
				}
			}
		}
		*/
	}

	public void stitchAndGenerateAtlas(String id) {
		long totalArea = 0;
		Map<BaseIcon,STBITexture> texDiff = new HashMap<>();
		Map<BaseIcon,STBITexture> texNorm = new HashMap<>();
		Map<BaseIcon,STBITexture> texPBR = new HashMap<>();
		TIntObjectMap<BaseIcon> idMap = new TIntObjectHashMap<>();

		STBRPRect.Buffer rect_list = STBRPRect.malloc(iconList.size());
		try(MemoryStack stack = stackPush()) {

			int idx = 0;

			int min_size = Integer.MAX_VALUE;

			for (BaseIcon icon : iconList) {
				STBITexture diff = new STBITexture(refIconDiff.get(icon));
				STBITexture norm = isSingleTexture ? null : new STBITexture(refIconNorm.get(icon));
				STBITexture pbr = isSingleTexture ? null : new STBITexture(refIconPBR.get(icon));
				texDiff.put(icon, diff);
				texNorm.put(icon, norm);
				texPBR.put(icon, pbr);
				totalArea += diff.width * diff.height;
				if(!isSingleTexture) {
					if (diff.width != norm.width || diff.height != norm.height
							    || diff.width != pbr.width || diff.height != pbr.height) {
						CrashReport crashReport = new CrashReport("Textures not of same size!");
						crashReport.invalidState(refIconDiff.get(icon).getResourceID());
						OpenVoxel.reportCrash(crashReport);
					}
				}

				min_size = Math.min(min_size,Math.min(diff.width,diff.height));

				idMap.put(idx, icon);
				rect_list.position(idx);
				rect_list.id(idx);
				rect_list.w((short) diff.width);
				rect_list.h((short) diff.height);
				rect_list.x((short) 0);
				rect_list.y((short) 0);
				rect_list.was_packed(false);
				idx += 1;
			}
			rect_list.position(0);

			AtlasMipLevels = (int)Math.round(Math.log(min_size) / Math.log(2));

			STBRPContext context = STBRPContext.mallocStack(stack);

			int minDim = (int) Math.sqrt((double) totalArea);
			int startingPower = MathUtilities.roundUpToNearestPowerOf2(minDim);
			while (startingPower > 0) {
				//Attempt with size = startingPower...
				//TODO: FIX PROPERLY
				//STBRPNode.Buffer node_buffer = STBRPNode.malloc(startingPower);
				STBRPNode.Buffer node_buffer = STBRPNode.createSafe(MemoryUtil.nmemAlloc(STBRPNode.SIZEOF * startingPower),startingPower);
				stbrp_init_target(context, startingPower, startingPower, node_buffer);

				stbrp_setup_heuristic(context, STBRP_HEURISTIC_Skyline_default);

				int result = stbrp_pack_rects(context,rect_list);
				node_buffer.free();

				//Success...
				if (result != 0) {
					break;
				}

				//Fail...
				startingPower *= 2;
			}

			if (startingPower < 0) throw new RuntimeException("Failed to pack texture atlas");

			AtlasWidth = startingPower;
			AtlasHeight = startingPower;
			int AllocationSize = 4 * ( ((4 * AtlasHeight * AtlasWidth) - 1) / 3  );

			DataDiff = MemoryUtil.memCalloc(AllocationSize);
			if(!isSingleTexture) {
				DataNorm = MemoryUtil.memCalloc(AllocationSize);
				DataPBR = MemoryUtil.memCalloc(AllocationSize);
			}

			Logger logAtlas = Logger.getLogger("Atlas Stitching");
			logAtlas.Info("Creating with size (",AtlasWidth,",",AtlasHeight,")");

			float scale_factor = 1.0f / startingPower;
			for(int key = 0; key < idx; key++){
				rect_list.position(key);
				BaseIcon icon = idMap.get(key);

				logAtlas.Debug("Stored icon at (",rect_list.x(),",",rect_list.y(),")"
				,", size = (",rect_list.w(),",",rect_list.h(),")");
				//Update Icon
				icon.U0 = rect_list.x() * scale_factor;
				icon.V0 = rect_list.y() * scale_factor;
				icon.U1 = icon.U0 + (rect_list.w() * scale_factor);
				icon.V1 = icon.V0 + (rect_list.h() * scale_factor);
				icon.animationCount = Math.floorDiv(rect_list.h(),rect_list.w());

				//Store Image Source
				STBITexture diff = texDiff.remove(icon);
				STBITexture norm = texNorm.remove(icon);
				STBITexture pbr = texPBR.remove(icon);
				for (int y_off = 0; y_off < rect_list.h(); y_off++) {
					int offset = y_off * diff.width;
					int target_off = (rect_list.y() + y_off) * AtlasWidth;
					for (int x_off = 0; x_off < rect_list.w(); x_off++) {
						int loc = 4 * (offset + x_off);
						int target_loc = 4 * (rect_list.x() + target_off + x_off);
						DataDiff.putInt(target_loc, diff.pixels.getInt(loc));
						if(!isSingleTexture) {
							DataNorm.putInt(target_loc, norm.pixels.getInt(loc));
							DataPBR.putInt(target_loc, pbr.pixels.getInt(loc));
						}
					}
				}
				diff.Free();
				if(!isSingleTexture) {
					norm.Free();
					pbr.Free();
				}
			}
		}finally {
			rect_list.free();
		}
		//Generate Mip Maps
		int old_mip_offset = 0;
		int old_mip_size = AtlasWidth;
		int mip_offset = AtlasHeight * AtlasWidth;
		int mip_size = AtlasWidth / 2;
		while(mip_size > 0) {
			/*
			long ptr_input = MemoryUtil.memAddress(DataDiff,4 * old_mip_offset);
			long ptr_output = MemoryUtil.memAddress(DataDiff, 4 * mip_offset);
			resize(ptr_input,old_mip_size,ptr_output,mip_size);
			if(!isSingleTexture) {
				ptr_input = MemoryUtil.memAddress(DataNorm, 4 * old_mip_offset);
				ptr_output = MemoryUtil.memAddress(DataNorm, 4 * mip_offset);
				resize(ptr_input,old_mip_size,ptr_output,mip_size);
				ptr_input = MemoryUtil.memAddress(DataPBR, 4 * old_mip_offset);
				ptr_output = MemoryUtil.memAddress(DataPBR, 4 * mip_offset);
				resize(ptr_input,old_mip_size,ptr_output,mip_size);
			}*/
			int old_byte_offset = 4 * old_mip_offset;
			int new_byte_offset = 4 * mip_offset;
			resize(DataDiff,old_byte_offset,old_mip_size,new_byte_offset,mip_size);
			if(!isSingleTexture) {
				resize(DataNorm,old_byte_offset,old_mip_size,new_byte_offset,mip_size);
				resize(DataPBR,old_byte_offset,old_mip_size,new_byte_offset,mip_size);
			}

			//Next Mip
			old_mip_size = mip_size;
			old_mip_offset = mip_offset;
			mip_offset += mip_size * mip_size;
			mip_size /= 2;
		}

		if(!isSingleTexture) {
			FolderUtils.saveTextureStitch(AtlasWidth, AtlasHeight, DataDiff, id + "-diffuse", AtlasMipLevels);
			FolderUtils.saveTextureStitch(AtlasWidth, AtlasHeight, DataNorm, id + "-normal", AtlasMipLevels);
			FolderUtils.saveTextureStitch(AtlasWidth, AtlasHeight, DataPBR, id + "-pbr", AtlasMipLevels);
		}else{
			FolderUtils.saveTextureStitch(AtlasWidth,AtlasHeight,DataDiff,id+"-stitch",AtlasMipLevels);
		}
	}

	public void freeAtlas() {
		MemoryUtil.memFree(DataDiff);
		DataDiff = null;
		if(!isSingleTexture) {
			MemoryUtil.memFree(DataNorm);
			MemoryUtil.memFree(DataPBR);
			DataNorm = null;
			DataPBR = null;
		}
	}

}
