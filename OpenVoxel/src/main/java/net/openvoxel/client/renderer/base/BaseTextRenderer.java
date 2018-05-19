package net.openvoxel.client.renderer.base;

import com.jc.util.format.json.JSONList;
import com.jc.util.format.json.JSONMap;
import com.jc.util.format.json.JSONObject;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.openvoxel.OpenVoxel;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.utility.CrashReport;

public abstract class BaseTextRenderer {

	public ResourceHandle handle;
	private static class FontGlyph {
		private int ID;
		private int x;
		private int y;
		private int width;
		private int height;
		private int xOffset;
		private int yOffset;
		private int xAdvance;

		private float U0;
		private float U1;
		private float V0;
		private float V1;

		private TIntIntMap kerningMap = new TIntIntHashMap();
	}
	private TIntObjectMap<FontGlyph> glyphMap;
	private FontGlyph missingGlyph;
	private float lineHeight;
	//private float capHeight;

	public BaseTextRenderer(String imgRes) {
		handle = ResourceManager.getImage(imgRes);
		glyphMap = new TIntObjectHashMap<>();
		missingGlyph = null;
		loadMetadata();
	}

	protected void loadMetadata() {
		JSONObject json = handle.getMetadata();
		if(json == null) {
			CrashReport crash = new CrashReport("Text Images require Metadata!!");
			crash.invalidState("handle.getMetadata() == null");
			OpenVoxel.reportCrash(crash);
			return;
		}
		String metaType = json.asMap().get("type").asString();
		switch (metaType) {
			case "angel_font":
				loadUpdatedMetadata(json);
				break;
			case "grid":
				loadLegacyMetadata(json);
				break;
			default:
				CrashReport crash = new CrashReport("Unknown String metadata");
				crash.invalidState("json['type'] == " + metaType);
				OpenVoxel.reportCrash(crash);
				break;
		}
	}

	private void loadLegacyMetadata(JSONObject json) {
		int numCellsX = json.asMap().get("num_cells_horizontal").asInteger();
		int numCellsY = json.asMap().get("num_cells_vertical").asInteger();
		int pixelsPerCell = json.asMap().get("pixels_per_cell").asInteger();
		int charCodeOffset = json.asMap().get("char_code_point_offset").asInteger();
		JSONList list = json.asMap().get("cell_pixel_widths").asList();
		int[] pixelWidths = new int[list.size()];
		for(int i = 0; i < list.size(); i++) {
			pixelWidths[i] = list.get(i).asInteger();
		}
		float cellWidth = 1.0f / numCellsX;
		float cellHeight = 1.0f / numCellsY;

		for(int i = 0; i < charCodeOffset; i++) {
			FontGlyph glyph = new FontGlyph();
			glyph.ID = i;
			glyph.xAdvance = pixelWidths[i];
			glyph.xOffset = 0;
			glyph.yOffset = 0;
			glyph.x = 0;
			glyph.y = 0;
			glyph.width = pixelWidths[i];
			glyph.height = pixelsPerCell;

			float Width = ((float)pixelWidths[i] / pixelsPerCell);

			glyph.U0 = 0;
			glyph.U1 = glyph.U0 + (Width * cellWidth);
			glyph.V1 = 0;
			glyph.V0 = glyph.V1 + cellHeight;

			glyphMap.put(i,glyph);
		}

		for(int i = charCodeOffset; i < list.size(); i++) {
			FontGlyph glyph = new FontGlyph();
			glyph.ID = i;
			int charID =  i - charCodeOffset;
			int YCell = charID / numCellsX;
			int XCell = charID % numCellsX;

			float Width = ((float)pixelWidths[i] / pixelsPerCell);

			glyph.xAdvance = pixelWidths[i];
			glyph.xOffset = 0;
			glyph.yOffset = 0;
			glyph.x = 0;
			glyph.y = 0;
			glyph.width = pixelWidths[i];
			glyph.height = pixelsPerCell;

			glyph.U0 = XCell * cellWidth;
			glyph.U1 = glyph.U0 + (Width * cellWidth);
			glyph.V1 = YCell * cellHeight;
			glyph.V0 = glyph.V1 + cellHeight;

			glyphMap.put(i,glyph);
		}
		lineHeight = pixelsPerCell;

		missingGlyph = glyphMap.get(0);
	}

	private void loadUpdatedMetadata(JSONObject json) {
		JSONList padding = json.asMap().get("info").asMap().get("padding").asList();
		//float paddingTop = padding.get(0).asInteger();
		float paddingRight = padding.get(1).asInteger();
		//float paddingBottom = padding.get(2).asInteger();
		float paddingLeft = padding.get(3).asInteger();
		//float paddingY = paddingTop + paddingBottom;
		JSONMap commonData = json.asMap().get("common").asMap();
		lineHeight = commonData.get("lineHeight").asInteger();
		//float baseLine = commonData.asMap().get("base").asInteger();
		float texWidth = commonData.get("scaleW").asInteger();
		float texHeight = commonData.get("scaleH").asInteger();

		float invTexWidth = 1.0F / texWidth;
		float invTexHeight = 1.0F / texHeight;
		//float descent = 0;

		JSONList glyphList = json.asMap().get("chars").asList();
		for(int i = 0; i < glyphList.size(); i++) {
			JSONMap glyphData = glyphList.get(i).asMap();
			FontGlyph glyph = new FontGlyph();
			glyph.ID = glyphData.get("id").asInteger();
			if (glyph.ID <= 0) {
				missingGlyph = glyph;
			}else if (glyph.ID <= Character.MAX_VALUE) {
				glyphMap.put(glyph.ID, glyph);
			}else {
				continue;
			}
			glyph.x = glyphData.get("x").asInteger();
			glyph.y = glyphData.get("y").asInteger();
			glyph.width = glyphData.get("width").asInteger();
			glyph.height = glyphData.get("height").asInteger();
			glyph.xOffset = glyphData.get("x-offset").asInteger();
			glyph.yOffset = glyphData.get("y-offset").asInteger();
			glyph.xAdvance = glyphData.get("x-advance").asInteger();
			//if (glyph.width > 0 && glyph.height > 0) descent = Math.min(baseLine + glyph.yOffset, descent);

			//Calculate GlyphLocation
			float pos_x1 = glyph.x;
			float pos_x2 = glyph.x + glyph.width;
			float pos_y1 = glyph.y;
			float pos_y2 = glyph.y + glyph.height;

			glyph.U0 = 0 + pos_x1 * invTexWidth;
			glyph.U1 = 0 + pos_x2 * invTexWidth;
			glyph.V1 = 0 + pos_y1 * invTexHeight;
			glyph.V0 = 0 + pos_y2 * invTexHeight;
		}

		//descent += paddingBottom;

		//Load Kerning...
		JSONList kerningList = json.asMap().get("kernings").asList();
		for(int i = 0; i < kerningList.size(); i++) {
			JSONMap kerningData = kerningList.get(i).asMap();
			int first = kerningData.get("first").asInteger();
			int second = kerningData.get("second").asInteger();
			int amount = kerningData.get("amount").asInteger();
			FontGlyph firstGlyph = glyphMap.get(first);
			if(firstGlyph != null) {
				firstGlyph.kerningMap.put(second,amount);
			}
		}

		//Handle Space Glyph (Possible to be missing)
		FontGlyph spaceGlyph = glyphMap.get((int)' ');
		if(spaceGlyph == null) {
			spaceGlyph = new FontGlyph();
			spaceGlyph.ID = (int)' ';
			FontGlyph xAdvanceGlyph = glyphMap.get((int)'l');
			spaceGlyph.xAdvance = xAdvanceGlyph.xAdvance;
			glyphMap.put(spaceGlyph.ID,spaceGlyph);
		}
		if (spaceGlyph.width == 0) {
			spaceGlyph.width = (int)(paddingLeft + spaceGlyph.xAdvance + paddingRight);
			spaceGlyph.xOffset = (int)-paddingLeft;
		}

		//Handle missing glyph
		if(missingGlyph == null) {
			missingGlyph = spaceGlyph;
		}
		glyphMap.put(0,missingGlyph);
	}

	private float DrawChar(BaseGuiRenderer renderer,float X, float Y, float Height,
	                       FontGlyph glyph,FontGlyph lastGlyph,float aspectRatio,int col) {
		final float yScale = Height / lineHeight;
		final float xScale = yScale * aspectRatio;

		//Calculate Kerning
		int kerning = 0;
		if(lastGlyph != null) {
			kerning = lastGlyph.kerningMap.get(glyph.ID);
			if(kerning == lastGlyph.kerningMap.getNoEntryValue()) kerning = 0;
		}

		//Scale Values
		final float kerningOffset = xScale * kerning;
		final float advanceWidth = xScale * glyph.xAdvance;
		final float glyphWidth = xScale * glyph.width;
		final float glyphHeight = yScale * glyph.height;
		final float glyphXOffset = xScale * glyph.xOffset;
		final float glyphYOffset = yScale * glyph.yOffset;

		float minX = X + kerningOffset + glyphXOffset;
		float maxX = minX + glyphWidth;

		float maxY = Y + glyphYOffset - Height;
		float minY = maxY + glyphHeight;

		renderer.VertexRect(maxX,minX,minY,maxY,glyph.U1,glyph.U0,glyph.V0,glyph.V1,col);


//		renderer.VertexWithColUV(maxX,maxY,glyph.U1,glyph.V1,col);
//		renderer.VertexWithColUV(minX,maxY,glyph.U0,glyph.V1,col);
//		renderer.VertexWithColUV(minX,minY,glyph.U0,glyph.V0,col);
//
//		renderer.VertexWithColUV(maxX,minY,glyph.U1,glyph.V0,col);
//		renderer.VertexWithColUV(maxX,maxY,glyph.U1,glyph.V1,col);
//		renderer.VertexWithColUV(minX,minY,glyph.U0,glyph.V0,col);

		return advanceWidth + kerningOffset;
	}

	public void DrawText(BaseGuiRenderer draw,int screenWidth, int screenHeight,
	                     float x, float y, float height, CharSequence text, int col) {
		draw.Begin(handle);
		//Draw Vertex List
		final int SIZE = text.length();
		final float aspect = (float)screenHeight / (float)screenWidth;

		FontGlyph lastGlyph = null;
		float runningOffset = 0;
		for(int i = 0; i < SIZE; i++){
			char c = text.charAt(i);
			FontGlyph glyph = glyphMap.get(c);
			if(glyph == null) glyph = missingGlyph;
			runningOffset += DrawChar(draw,x+runningOffset,y,height,glyph,lastGlyph,aspect,col);
			lastGlyph = glyph;
		}
	}

	public float GetTextWidthRatio(CharSequence text,int screenWidth,int screenHeight) {
		final int SIZE = text.length();
		final float aspect = (float)screenHeight / (float)screenWidth;

		FontGlyph lastGlyph = null;
		float runningOffset = 0;
		for(int i = 0; i < SIZE; i++){
			int c = text.charAt(i);
			FontGlyph glyph = glyphMap.get(c);
			if(glyph == null) glyph = missingGlyph;
			runningOffset += glyph.xAdvance;
			if(lastGlyph != null) {
				int kerning = lastGlyph.kerningMap.get(c);
				if(kerning != lastGlyph.kerningMap.getNoEntryValue()) {
					runningOffset += kerning;
				}
			}
			//TODO: OFFSET AT START???
			lastGlyph = glyph;
		}
		return runningOffset * aspect / lineHeight;
	}
}
