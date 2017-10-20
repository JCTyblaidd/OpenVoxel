package net.openvoxel.client.renderer.vk;

import net.openvoxel.api.logger.Logger;
import net.openvoxel.client.STBITexture;
import net.openvoxel.client.textureatlas.Icon;
import net.openvoxel.client.textureatlas.IconAtlas;
import net.openvoxel.common.resources.ResourceHandle;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VkTexAtlas implements IconAtlas {

	private List<VkTexIcon> texIcons = new ArrayList<>();
	private ByteBuffer pack_diffuse;
	private ByteBuffer pack_normal;
	private ByteBuffer pack_pbr;


	void cleanup() {
		MemoryUtil.memFree(pack_diffuse);
		MemoryUtil.memFree(pack_normal);
		MemoryUtil.memFree(pack_pbr);
	}

	@Override
	public Icon register(ResourceHandle handle_diffuse, ResourceHandle handle_normal, ResourceHandle handle_pbr) {
		VkTexIcon icon = new VkTexIcon(handle_diffuse,handle_normal,handle_pbr);
		texIcons.add(icon);
		return icon;
	}

	@Override
	public void performStitch() {
		texIcons.forEach(VkTexIcon::load_textures);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			STBRPContext context = STBRPContext.mallocStack(stack);
			STBRectPack.stbrp_setup_heuristic(context,STBRectPack.STBRP_HEURISTIC_Skyline_default);
			STBRectPack.stbrp_setup_allow_out_of_mem(context,false);
			final int width = 0, height = 0;
			final int node_scale = 32;
			STBRPNode.Buffer nodes = STBRPNode.mallocStack(width*node_scale,stack);
			STBRectPack.stbrp_init_target(context,width,height,nodes);
			STBRPRect.Buffer rectangles = STBRPRect.mallocStack(texIcons.size(),stack);
			for(int i = 0; i < texIcons.size(); i++) {
				rectangles.position(i);
				VkTexIcon icon = texIcons.get(i);
				rectangles.id(i);
				rectangles.w((short)icon.tex_diffuse.width);
				rectangles.h((short)icon.tex_diffuse.height);
			}
			rectangles.position(0);
			STBRectPack.stbrp_pack_rects(context,rectangles);
			int maxWidth = 0, maxHeight = 0;
			for(int i = 0; i < texIcons.size(); i++) {
				rectangles.position(i);
				int t_width = rectangles.x() + rectangles.w();
				int t_height = rectangles.y() + rectangles.h();
				if(maxWidth < t_width) maxWidth = t_width;
				if(maxHeight < t_height) maxHeight = t_height;
			}
			Logger.getLogger("Vulkan Icon Packer").Info("Width: " + maxWidth + ", Height: " + maxHeight);
			pack_diffuse = MemoryUtil.memAlloc(maxWidth * maxHeight * 4);
			pack_normal = MemoryUtil.memAlloc(maxWidth * maxHeight * 4);
			pack_pbr = MemoryUtil.memAlloc(maxWidth * maxHeight * 4);
			float widthFactor = 1.0F/maxWidth, heightFactor = 1.0F/maxHeight;
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
			}
		}
		texIcons.forEach(VkTexIcon::unload_textures);
	}


	private static class VkTexIcon implements Icon {
		private ResourceHandle diffuse;
		private ResourceHandle normal;
		private ResourceHandle pbr;
		private STBITexture tex_diffuse;
		private STBITexture tex_normal;
		private STBITexture tex_pbr;
		private float u_min, u_max, v_min, v_max;
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
