package net.openvoxel.client.renderer.base;

import com.jc.util.format.json.JSONList;
import com.jc.util.format.json.JSONObject;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.logger.Logger;
import net.openvoxel.common.resources.ResourceHandle;
import net.openvoxel.common.resources.ResourceManager;
import net.openvoxel.utility.CrashReport;

public abstract class BaseTextRenderer {

	public ResourceHandle handle;
	private int numCellsX;
	private int numCellsY;
	private float cellWidth;
	private float cellHeight;
	private int pixelsPerCell;
	private int charCodeOffset;
	private int[] pixelWidths;

	public BaseTextRenderer(String imgRes) {
		handle = ResourceManager.getImage(imgRes);
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
		numCellsX = json.asMap().get("num_cells_horizontal").asInteger();
		numCellsY = json.asMap().get("num_cells_vertical").asInteger();
		pixelsPerCell = json.asMap().get("pixels_per_cell").asInteger();
		charCodeOffset = json.asMap().get("char_code_point_offset").asInteger();
		JSONList list = json.asMap().get("cell_pixel_widths").asList();
		pixelWidths = new int[list.size()];
		for(int i = 0; i < list.size(); i++) {
			pixelWidths[i] = list.get(i).asInteger();
		}

		cellWidth = 1.0f / numCellsX;
		cellHeight = 1.0f / numCellsY;
	}



	private float DrawChar(BaseGuiRenderer renderer,float X, float Y, float Height,char c,float aspectRatio,int col) {
		int charID = ((int)c)-charCodeOffset;

		int YCell = charID / numCellsX;
		int XCell = charID % numCellsX;

		float minU = XCell * cellWidth;
		float Width = ((float)pixelWidths[(int)c] / pixelsPerCell);
		float maxU = minU + (Width * cellWidth);
		float maxV = YCell * cellHeight;
		float minV = maxV + cellHeight;

		float realWidth = Height * Width * aspectRatio;
		float maxY = Y - Height;
		float maxX = X + realWidth;

		renderer.VertexWithColUV(X,Y,minU,minV,col);
		renderer.VertexWithColUV(X,maxY,minU,maxV,col);
		renderer.VertexWithColUV(maxX,maxY,maxU,maxV,col);

		renderer.VertexWithColUV(X,Y,minU,minV,col);
		renderer.VertexWithColUV(maxX,maxY,maxU,maxV,col);
		renderer.VertexWithColUV(maxX,Y,maxU,minV,col);

		return realWidth;
	}

	public void DrawText(BaseGuiRenderer draw,int screenWidth, int screenHeight,
	                     float x, float y, float height, CharSequence text, int col) {
		draw.Begin(handle);
		//Draw Vertex List
		final int SIZE = text.length();
		float runningOffset = 0;
		final float aspect = (float)screenHeight / screenWidth;
		for(int i = 0; i < SIZE; i++){
			char c = text.charAt(i);
			runningOffset += DrawChar(draw,x+runningOffset,y,height,c,aspect,col);
		}
	}

	public float GetTextWidthRatio(CharSequence text,int screenWidth,int screenHeight) {
		final int SIZE = text.length();
		final float aspect = (float)screenHeight / (float)screenWidth;
		float runningOffset = 0;
		for(int i = 0; i < SIZE; i++){
			int c = text.charAt(i);
			float width = pixelWidths[c];
			runningOffset += (width / pixelsPerCell) * aspect;
		}
		return runningOffset;
	}


}
