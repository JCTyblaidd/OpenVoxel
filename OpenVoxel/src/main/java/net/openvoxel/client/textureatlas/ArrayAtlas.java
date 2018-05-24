package net.openvoxel.client.textureatlas;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.client.STBITexture;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.utility.CrashReport;
import net.openvoxel.utility.MathUtilities;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayAtlas implements IconAtlas {

	private List<ArrayIcon> iconList = new ArrayList<>();
	private Map<ArrayIcon,ResourceHandle> refIconDiff = new HashMap<>();
	private Map<ArrayIcon,ResourceHandle> refIconNorm = new HashMap<>();
	private Map<ArrayIcon,ResourceHandle> refIconPBRD = new HashMap<>();

	//Stitch State
	private TIntIntMap sizeCountMap = new TIntIntHashMap();
	private Map<ArrayIcon,STBITexture> texDiff = new HashMap<>();
	private Map<ArrayIcon,STBITexture> texNorm = new HashMap<>();
	private Map<ArrayIcon,STBITexture> texPBRD = new HashMap<>();

	//Generation State
	private TIntList arraySizes = new TIntArrayList();
	private TIntList arrayLayers = new TIntArrayList();
	private List<List<ArrayIcon>> arrayTexMap = new ArrayList<>();

	@Override
	public Icon register(ResourceHandle handle_diffuse, ResourceHandle handle_normal, ResourceHandle handle_pbr) {
		ArrayIcon ref = new ArrayIcon();
		iconList.add(ref);
		refIconDiff.put(ref,handle_diffuse);
		refIconNorm.put(ref,handle_normal);
		refIconPBRD.put(ref,handle_pbr);
		return ref;
	}

	@Override
	public void performStitch() {
		for(ArrayIcon icon : iconList) {
			STBITexture diff = new STBITexture(refIconDiff.get(icon));
			STBITexture norm = new STBITexture(refIconNorm.get(icon));
			STBITexture pbrd  = new STBITexture(refIconPBRD.get(icon));
			texDiff.put(icon, diff);
			texNorm.put(icon, norm);
			texPBRD.put(icon, pbrd);

			//Validate Of the same size
			if(diff.width != norm.width || diff.height != norm.height ||
			   diff.width != pbrd.width || diff.height != pbrd.height ) {
				CrashReport crashReport = new CrashReport("Textures not of same size!");
				crashReport.invalidState(refIconDiff.get(icon).getResourceID());
				OpenVoxel.reportCrash(crashReport);
			}

			//Add to size count map
			int animation_count = diff.height / diff.width;
			if(animation_count == 0) {
				CrashReport crashReport = new CrashReport("Texture with width > height");
				crashReport.invalidState(refIconDiff.get(icon).getResourceID());
				OpenVoxel.reportCrash(crashReport);
			}
			icon.animationCount = animation_count;
			icon.iconSize = MathUtilities.roundUpToNearestPowerOf2(diff.width);
			icon.arrayIdx = -1;
			icon.textureIdx = -1;
			sizeCountMap.adjustOrPutValue(icon.iconSize,icon.animationCount,icon.animationCount);
		}
	}

	/**
	 * @param max_layers the maximum number of layers valid for an array
	 * @param max_size the maximum size in pixels valid for an array
	 * @return the number of arrays generated
	 */
	public int generateArrays(int max_layers, int max_size) {
		for(ArrayIcon icon : iconList) {
			int idx = 0;
			int real_size = Math.min(icon.iconSize,max_size);
			for(; idx < arraySizes.size(); idx++) {
				int idx_size = arraySizes.get(idx);
				int idx_layer = arrayLayers.get(idx);
				if(real_size == idx_size && (icon.animationCount + idx_layer) < max_layers) {
					break;
				}
			}

			//Create New Array
			if(idx == arraySizes.size()) {
				arraySizes.add(real_size);
				arrayLayers.add(0);
				arrayTexMap.add(new ArrayList<>());
			}

			//Store Icon Data
			icon.textureIdx = arrayLayers.get(idx);
			icon.arrayIdx = idx;

			//Store Image Array Data
			arrayLayers.set(idx,icon.textureIdx+icon.animationCount);
			arrayTexMap.get(idx).add(icon);
		}
		return arraySizes.size();
	}

	public interface ArrayLayerCallback {
		void createArray(int arrayIndex,int imgLayerCount,int mipCount, int imgSize);
		void storeArray(int arrayIndex, int layerIndex, int mipIndex, int imgSize,
		                ByteBuffer diffuse, ByteBuffer normal, ByteBuffer pbr);
	}

	public void createImageArrays(ArrayLayerCallback callback, int maxMipLevel) {
		final int arrayCount = arrayLayers.size();
		int maxSize = 0;
		for(int i = 0; i < arrayCount; i++) {
			int layerCount = arrayLayers.get(i);
			int imageSize = arraySizes.get(i);
			int mipCount = Math.min(maxMipLevel,MathUtilities.Log2Integer(imageSize));
			maxSize = Math.max(maxSize,imageSize);
			callback.createArray(i,layerCount,mipCount,imageSize);
		}
		ByteBuffer mipDiffBuffer = MemoryUtil.memAlloc(maxSize * maxSize * 4);
		ByteBuffer mipNormBuffer = MemoryUtil.memAlloc(maxSize * maxSize * 4);
		ByteBuffer mipPBRBuffer = MemoryUtil.memAlloc(maxSize * maxSize * 4);
		for(int i = 0; i < arrayCount; i++) {
			int layerIndex = 0;
			int imageSize = arraySizes.get(i);
			int mipCount = Math.min(maxMipLevel,MathUtilities.Log2Integer(imageSize));
			int skipSize = imageSize * imageSize * 4;
			for(ArrayIcon icon : arrayTexMap.get(i)) {
				STBITexture texDiffuse = texDiff.get(icon);
				STBITexture texNormal = texNorm.get(icon);
				STBITexture texPBR = texPBRD.get(icon);
				for(int frame = 0; frame < icon.animationCount; frame++) {
					//Set Image Data Offsets
					System.out.println(frame+","+skipSize+","+icon.animationCount);
					texDiffuse.pixels.position(frame * skipSize);
					texNormal.pixels.position(frame * skipSize);
					texPBR.pixels.position(frame * skipSize);

					//Store Image Data
					callback.storeArray(
							i,
							layerIndex,
							0,
							imageSize,
							texDiffuse.pixels,
							texNormal.pixels,
							texPBR.pixels
					);

					//Generate And Store Image Mip Map Data
					int mipSize = imageSize;
					for(int mip = 1; mip < mipCount; mip++) {
						mipSize /= 2;
						STBImageResize.stbir_resize_uint8(
							texDiffuse.pixels,
							imageSize,
							imageSize,
							0,
							mipDiffBuffer,
							mipSize,
							mipSize,
							0,
							4
						);
						STBImageResize.stbir_resize_uint8(
							texNormal.pixels,
							imageSize,
							imageSize,
							0,
							mipNormBuffer,
							mipSize,
							mipSize,
							0,
							4
						);
						STBImageResize.stbir_resize_uint8(
							texPBR.pixels,
							imageSize,
							imageSize,
							0,
							mipNormBuffer,
							mipSize,
							mipSize,
							0,
							4
						);
						callback.storeArray(
							i,
							layerIndex,
							mip,
							mipSize,
							mipDiffBuffer,
							mipNormBuffer,
							mipPBRBuffer
						);
					}
					layerIndex++;
				}
			}
		}
		MemoryUtil.memFree(mipDiffBuffer);
		MemoryUtil.memFree(mipNormBuffer);
		MemoryUtil.memFree(mipPBRBuffer);
	}


	/**
	 * Free all data allocated by the texture stitching
	 */
	public void freeAllTextures() {
		texDiff.forEach((k,v) -> v.Free());
		texNorm.forEach((k,v) -> v.Free());
		texPBRD.forEach((k,v) -> v.Free());
		texDiff.clear();
		texNorm.clear();
		texPBRD.clear();
		sizeCountMap.clear();

		arrayLayers.clear();
		arraySizes.clear();
		arrayTexMap.clear();
	}

	public static class ArrayIcon implements Icon {

		public ArrayIcon() {
			arrayIdx = 0;
			textureIdx = 0;
			animationCount = 1;
			iconSize = 1;
		}

		public int arrayIdx;
		public int textureIdx;
		public int animationCount;
		public int iconSize;
	}
}
