package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.STBITexture;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.files.FolderUtils;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.stb.STBImageResize.stbir_resize_uint8;
import static org.lwjgl.system.MemoryStack.stackPush;

public class VkTexAtlas implements IconAtlas {

	private List<VkTexIcon> texIcons = new ArrayList<>();
	//Mip levels are stored inside of the packed data
	public ByteBuffer pack_diffuse;
	public ByteBuffer pack_normal;
	public ByteBuffer pack_pbr;
	public int pack_width;
	public int expanded_pack_width;
	public int pack_height;
	public int pack_mip_count;

	void cleanup() {
		if(pack_diffuse != null) {
			MemoryUtil.memFree(pack_diffuse);
			MemoryUtil.memFree(pack_normal);
			MemoryUtil.memFree(pack_pbr);
			pack_diffuse = null;
			pack_normal = null;
			pack_pbr = null;
		}
	}

	@Override
	public Icon register(ResourceHandle handle_diffuse, ResourceHandle handle_normal, ResourceHandle handle_pbr) {
		VkTexIcon icon = new VkTexIcon(handle_diffuse,handle_normal,handle_pbr);
		texIcons.add(icon);
		return icon;
	}

	@Override
	public void performStitch() {
		VkRenderer.Vkrenderer.markAsRegenRequired();
	}

	private int round_up_to_next(int to_round,int round_target) {
		int div_up = (to_round + round_target - 1) / round_target;
		return div_up * round_target;
	}

	private void gen_rectangles(STBRPRect.Buffer rectangles) {
		for(int i = 0; i < texIcons.size(); i++) {
			rectangles.position(i);
			VkTexIcon icon = texIcons.get(i);
			rectangles.id(i);
			rectangles.w((short)icon.tex_diffuse.width);
			rectangles.h((short)icon.tex_diffuse.height);
		}
		rectangles.position(0);
	}

	private void store_target_data(STBRPRect.Buffer rectangles,int maxWidth, int maxHeight,int maxElemSize) {
		float widthFactor  = 1.0F / maxWidth;
		float heightFactor = 1.0F / maxHeight;

		ByteBuffer outStorage1 = MemoryUtil.memAlloc(maxElemSize * 4);
		ByteBuffer outStorage2 = MemoryUtil.memAlloc(maxElemSize * 4);
		ByteBuffer outStorage3 = MemoryUtil.memAlloc(maxElemSize * 4);

		for(int i = 0; i < texIcons.size(); i++) {
			rectangles.position(i);
			VkTexIcon icon = texIcons.get(i);
			int t_x = rectangles.x();
			int t_y = rectangles.y();
			int t_width = rectangles.x() + rectangles.w();
			int t_height = rectangles.y() + rectangles.h();
			icon.u_min = t_x * widthFactor;
			icon.v_min = t_y * heightFactor;
			icon.u_max = t_width * widthFactor;
			icon.v_max = t_height * heightFactor;
			for(int mip_target = 0; mip_target < pack_mip_count; mip_target++) {
				int mip_x_offset = mip_target == 0 ? 0 : pack_width;
				int mip_y_offset = 0;
				for(int mip_iterate = 2; mip_iterate <= mip_target; mip_iterate++) {
					int offset_sf = (1 << (mip_iterate - 1));
					mip_y_offset += (pack_height + offset_sf - 1) / offset_sf;
				}
				int mip_scale = (1 << mip_target);
				int reduced_height = t_width / mip_scale;
				int reduced_width = t_width / mip_scale;
				if(mip_target == 0) {
					MemoryUtil.memCopy(icon.tex_diffuse.pixels,outStorage1);
					MemoryUtil.memCopy(icon.tex_normal.pixels,outStorage2);
					MemoryUtil.memCopy(icon.tex_pbr.pixels,outStorage3);
				}else {
					stbir_resize_uint8(icon.tex_diffuse.pixels, rectangles.w(), rectangles.h(), 0,
							outStorage1, reduced_width, reduced_height, 0, 4);
					stbir_resize_uint8(icon.tex_normal.pixels, rectangles.w(), rectangles.h(), 0,
							outStorage2, reduced_width, reduced_height, 0, 4);
					stbir_resize_uint8(icon.tex_pbr.pixels, rectangles.w(), rectangles.h(), 0,
							outStorage3, reduced_width, reduced_height, 0, 4);
				}

				for (int x_target = t_x; x_target < reduced_width; x_target++) {
					for (int y_target = t_y; y_target < reduced_height; y_target++) {
						int source_index = 4 * ((x_target - t_x) + (y_target - t_y) * reduced_width);
						int target_index = 4 * ((x_target+mip_x_offset) + (y_target+mip_y_offset) * expanded_pack_width);

						pack_diffuse.putInt(target_index,outStorage1.getInt(source_index));
						pack_normal.putInt(target_index,outStorage2.getInt(source_index));
						pack_pbr.putInt(target_index,outStorage3.getInt(source_index));
					}
				}
			}
		}
		MemoryUtil.memFree(outStorage1);
		MemoryUtil.memFree(outStorage2);
		MemoryUtil.memFree(outStorage3);
	}

	void performStitchInternal() {
		texIcons.forEach(VkTexIcon::load_textures);
		try(MemoryStack stack = stackPush()) {

			//Pack the images onto the texture atlas
			STBRPContext context = STBRPContext.callocStack(stack);
			final int dimension = Math.max(texIcons.size() * texIcons.size() / 2,4);
			final int target_dimension = texIcons.get(0).tex_diffuse.width * dimension;
			STBRPNode.Buffer nodes = STBRPNode.callocStack(dimension*dimension,stack);
			STBRectPack.stbrp_init_target(context,target_dimension,target_dimension,nodes);
			STBRectPack.stbrp_setup_heuristic(context,STBRectPack.STBRP_HEURISTIC_Skyline_default);
			STBRectPack.stbrp_setup_allow_out_of_mem(context,false);
			STBRPRect.Buffer rectangles = STBRPRect.callocStack(texIcons.size(),stack);
			gen_rectangles(rectangles);
			STBRectPack.stbrp_pack_rects(context,rectangles);

			//Determine State Data about packing target//
			int maxWidth = 0, maxHeight = 0;
			int maxElemMipCount = Integer.MAX_VALUE;
			int maxElemSize = 0;
			for(int i = 0; i < texIcons.size(); i++) {
				rectangles.position(i);
				int t_width = rectangles.x() + rectangles.w();
				int t_height = rectangles.y() + rectangles.h();
				if(maxWidth < t_width) maxWidth = t_width;
				if(maxHeight < t_height) maxHeight = t_height;
				int elem_mip_count = Math.min(Integer.numberOfTrailingZeros(rectangles.w()),
						Integer.numberOfTrailingZeros(rectangles.h()));
				if(elem_mip_count < maxElemMipCount) maxElemMipCount = elem_mip_count;
				int elem_size = rectangles.w() * rectangles.h();
				if(elem_size > maxElemSize) maxElemSize = elem_size;
			}

			//Update global storage variables//
			pack_mip_count = maxElemMipCount + 1;
			pack_width = round_up_to_next(maxWidth, 1 << maxElemMipCount);
			pack_height = round_up_to_next(maxHeight, 1 << maxElemMipCount);
			expanded_pack_width = (maxWidth * 3 + 1)/2;
			Logger.getLogger("Vulkan Icon Packer").Info("Width: " + maxWidth + ", Height: " + maxHeight + ", Mips: " + pack_mip_count);

			//Generate the final target data
			pack_diffuse = MemoryUtil.memCalloc(expanded_pack_width * pack_height * 4 );
			pack_normal = MemoryUtil.memCalloc(expanded_pack_width * pack_height * 4 );
			pack_pbr = MemoryUtil.memCalloc(expanded_pack_width * pack_height * 4 );
			store_target_data(rectangles,maxWidth,maxHeight,maxElemSize);

			//Output the resultant data//
			FolderUtils.saveTextureStitch(expanded_pack_width,pack_height,pack_diffuse,"diffuse-0");
			FolderUtils.saveTextureStitch(expanded_pack_width,pack_height,pack_normal,"normal-0");
			FolderUtils.saveTextureStitch(expanded_pack_width,pack_height,pack_pbr,"pbr-0");

		}finally {
			texIcons.forEach(VkTexIcon::unload_textures);
		}
	}


	public static class VkTexIcon implements Icon {
		private ResourceHandle diffuse;
		private ResourceHandle normal;
		private ResourceHandle pbr;
		private STBITexture tex_diffuse;
		private STBITexture tex_normal;
		private STBITexture tex_pbr;
		public float u_min, u_max, v_min, v_max;
		public VkTexIcon() {
			u_min = 0;
			u_max = 1;
			v_min = 0;
			v_max = 1;
		}
		private VkTexIcon(ResourceHandle diffuse,ResourceHandle normal,ResourceHandle pbr) {
			this.diffuse = diffuse;
			this.normal = normal;
			this.pbr = pbr;
		}
		private void load_textures() {
			diffuse.reloadData();
			normal.reloadData();
			pbr.reloadData();
			tex_diffuse = new STBITexture(diffuse.getByteData());
			tex_normal = new STBITexture(normal.getByteData());
			tex_pbr = new STBITexture(pbr.getByteData());
			if(tex_diffuse.height != tex_normal.height) {
				throw new RuntimeException("Error: Diffuse != Normal");
			}
			if(tex_normal.height != tex_pbr.height) {
				throw new RuntimeException("Error: Normal != PBR");
			}
			if(tex_diffuse.width != tex_normal.width) {
				throw new RuntimeException("Error: Diffuse != Normal");
			}
			if(tex_normal.width != tex_pbr.width) {
				throw new RuntimeException("Error: Normal != PBR");
			}
			diffuse.unloadData();
			normal.unloadData();
			pbr.unloadData();
		}
		private void unload_textures() {
			tex_diffuse.Free();
			tex_diffuse = null;
			tex_normal.Free();
			tex_normal = null;
			tex_pbr.Free();
			tex_pbr = null;
		}
	}
}
