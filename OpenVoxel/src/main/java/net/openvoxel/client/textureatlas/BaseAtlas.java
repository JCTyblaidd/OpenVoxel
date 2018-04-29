package net.openvoxel.client.textureatlas;

import net.openvoxel.common.resources.ResourceHandle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public void stitchAtlas() {
		//TODO: IMPLEMENT STBRectPack.pack...
	}

}
