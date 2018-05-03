package net.openvoxel.client.textureatlas;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.client.STBITexture;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.files.util.FolderUtils;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.debug.Validate;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.stb.STBRectPack.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class BaseAtlas {

	private boolean isSingleTexture;
	private List<BaseIcon> iconList;
	private Map<BaseIcon,ResourceHandle> refIconDiff;
	private Map<BaseIcon,ResourceHandle> refIconNorm;
	private Map<BaseIcon,ResourceHandle> refIconPBR;

	private int AtlasWidth;
	private int AtlasHeight;
	private int AtlasMipLevels;
	private ByteBuffer DataDiff;
	private ByteBuffer DataNorm;
	private ByteBuffer DataPBR;

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

	public BaseIcon registerAll(ResourceHandle diff,ResourceHandle norm,ResourceHandle pbr) {
		BaseIcon ref = new BaseIcon();
		iconList.add(ref);
		refIconDiff.put(ref,diff);
		refIconNorm.put(ref,norm);
		refIconPBR.put(ref,pbr);
		return ref;
	}

	private static int roundUpToPowerOf2(int x) {
		x = x - 1;
		x |= x >> 1;
		x |= x >> 2;
		x |= x >> 4;
		x |= x >> 8;
		x |= x >> 16;
		return x + 1;
	}


	public void stitchAndGenerateAtlas(String id) {
		long totalArea = 0;
		Map<BaseIcon,STBITexture> texDiff = new HashMap<>();
		Map<BaseIcon,STBITexture> texNorm = new HashMap<>();
		Map<BaseIcon,STBITexture> texPBR = new HashMap<>();
		TIntObjectMap<BaseIcon> idMap = new TIntObjectHashMap<>();

		STBRPRect.Buffer rect_list = STBRPRect.malloc(texDiff.size());
		try(MemoryStack stack = stackPush()) {

			int idx = 0;

			int min_size = Integer.MAX_VALUE;

			for (BaseIcon icon : iconList) {
				STBITexture diff = new STBITexture(refIconDiff.get(icon).getByteData());
				STBITexture norm = isSingleTexture ? null : new STBITexture(refIconNorm.get(icon).getByteData());
				STBITexture pbr = isSingleTexture ? null : new STBITexture(refIconPBR.get(icon).getByteData());
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
			int startingPower = roundUpToPowerOf2(minDim);
			while (startingPower > 0) {
				//Attempt with size = startingPower...
				//TODO: FIX PROPERLY
				//STBRPNode.Buffer node_buffer = STBRPNode.malloc(startingPower);
				STBRPNode.Buffer node_buffer = STBRPNode.createSafe(MemoryUtil.nmemAlloc(STBRPNode.SIZEOF * startingPower),startingPower);
				stbrp_init_target(context, startingPower, startingPower, node_buffer);

				stbrp_setup_heuristic(context, STBRP_HEURISTIC_Skyline_default);

				if (stbrp_pack_rects(context, rect_list) != 0) {
					break;
				}

				node_buffer.free();

				//Fail...
				startingPower *= 2;
			}

			if (startingPower < 0) throw new RuntimeException("Failed to pack texture atlas");

			AtlasWidth = startingPower;
			AtlasHeight = startingPower;
			int AllocationSize = ((4 * AtlasHeight * AtlasHeight) - 1) / 3;

			DataDiff = MemoryUtil.memAlloc(AllocationSize);
			if(!isSingleTexture) {
				DataNorm = MemoryUtil.memAlloc(AllocationSize);
				DataPBR = MemoryUtil.memAlloc(AllocationSize);
			}

			float scale_factor = 1.0f / startingPower;
			TIntObjectIterator<BaseIcon> icon_iter = idMap.iterator();
			while (icon_iter.hasNext()) {
				int key = icon_iter.key();
				rect_list.position(key);
				BaseIcon icon = icon_iter.value();

				//Update Icon
				icon.U0 = rect_list.x() * scale_factor;
				icon.V0 = rect_list.y() * scale_factor;
				icon.U1 = icon.U0 + (rect_list.w() * scale_factor);
				icon.V1 = icon.V0 + (rect_list.h() * scale_factor);

				//Store Image Source
				STBITexture diff = texDiff.remove(icon);
				STBITexture norm = texNorm.remove(icon);
				STBITexture pbr = texPBR.remove(icon);
				for (int y_off = 0; y_off < rect_list.h(); y_off++) {
					int offset = y_off * diff.width;
					int target_off = (rect_list.y() + y_off) * AtlasWidth;
					for (int x_off = 0; x_off < rect_list.w(); x_off++) {
						int loc = 4 * (offset + x_off);
						int target_loc = 4 * (target_off + x_off);
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
			long ptr_input = MemoryUtil.memAddress(DataDiff, old_mip_offset);
			long ptr_output = MemoryUtil.memAddress(DataDiff, mip_offset);
			STBImageResize.nstbir_resize_uint8(
					ptr_input, old_mip_size, old_mip_size, 0,
					ptr_output, mip_size, mip_size, 0,
					4
			);
			if(!isSingleTexture) {
				ptr_input = MemoryUtil.memAddress(DataNorm, old_mip_offset);
				ptr_output = MemoryUtil.memAddress(DataNorm, mip_offset);
				STBImageResize.nstbir_resize_uint8(
						ptr_input, old_mip_size, old_mip_size, 0,
						ptr_output, mip_size, mip_size, 0,
						4
				);
				ptr_input = MemoryUtil.memAddress(DataPBR, old_mip_offset);
				ptr_output = MemoryUtil.memAddress(DataPBR, mip_offset);
				STBImageResize.nstbir_resize_uint8(
						ptr_input, old_mip_size, old_mip_size, 0,
						ptr_output, mip_size, mip_size, 0,
						4
				);
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
