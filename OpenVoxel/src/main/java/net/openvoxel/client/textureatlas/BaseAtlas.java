package net.openvoxel.client.textureatlas;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.client.STBITexture;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.files.util.FolderUtils;
import net.openvoxel.utility.CrashReport;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;

public class BaseAtlas {

	private boolean isSingleTexture;
	private List<BaseIcon> iconList;
	private Map<BaseIcon,ResourceHandle> refIconDiff;
	private Map<BaseIcon,ResourceHandle> refIconNorm;
	private Map<BaseIcon,ResourceHandle> refIconPBR;

	private int AtlasWidth;
	private int AtlasHeight;
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

	public void stitchAtlas() {
		long totalArea = 0;
		Map<BaseIcon,STBITexture> texDiff = new HashMap<>();
		Map<BaseIcon,STBITexture> texNorm = new HashMap<>();
		Map<BaseIcon,STBITexture> texPBR = new HashMap<>();
		TIntObjectMap<BaseIcon> idMap = new TIntObjectHashMap<>();
		try(MemoryStack stack = stackPush()) {

			STBRPRect.Buffer rect_list = STBRPRect.mallocStack(texDiff.size(),stack);
			int idx = 0;

			for (BaseIcon icon : iconList) {
				STBITexture diff = new STBITexture(refIconDiff.get(icon).getByteData());
				STBITexture norm = new STBITexture(refIconNorm.get(icon).getByteData());
				STBITexture pbr = new STBITexture(refIconPBR.get(icon).getByteData());
				texDiff.put(icon, diff);
				texNorm.put(icon, norm);
				texPBR.put(icon, pbr);
				totalArea += diff.width * diff.height;
				if (diff.width != norm.width || diff.height != norm.height
						    || diff.width != pbr.width || diff.height != pbr.height) {
					CrashReport crashReport = new CrashReport("Textures not of same size!");
					crashReport.invalidState(refIconDiff.get(icon).getResourceID());
					OpenVoxel.reportCrash(crashReport);
				}

				idMap.put(idx,icon);
				rect_list.position(idx);
				rect_list.id(idx);
				rect_list.w((short)diff.width);
				rect_list.h((short)diff.height);
				rect_list.x((short)0);
				rect_list.y((short)0);
				rect_list.was_packed(false);
				idx += 1;
			}
			rect_list.position(0);

			STBRPContext context = STBRPContext.mallocStack(stack);

			int minDim = (int) Math.sqrt((double) totalArea);
			int startingPower = roundUpToPowerOf2(minDim);
			while (startingPower > 0) {
				//Attempt with size = startingPower...
				//STBRPNode.Buffer buffer = STBRPNode.
				//TODO: WHY DOES THIS NOT WORK?!?!?!?!
				//STBRectPack.stbrp_init_target(context,startingPower,startingPower,null);

				//Fail...
				startingPower *= 2;
			}
		}
	}

}
