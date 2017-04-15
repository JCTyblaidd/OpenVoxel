package net.openvoxel.client.renderer.gl3.util.shader;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Created by James on 09/04/2017.
 *
 * STD 140 Layout Generator
 */
public class STD140Layout {

	private TIntList offsetList;
	private int size;

	public enum LayoutType {
		BOOL(4,4),
		FLOAT(4,4),
		INT(4,4),
		VEC2(8,8),
		VEC3(16,16),
		VEC4(16,16),
		MAT2(16,8),
		MAT3(48,16),
		MAT4(64,16);
		private int size;
		private int alignment;
		LayoutType(int size, int alignment) {
			this.size = size;
			this.alignment = alignment;
		}
	}

	public STD140Layout(LayoutType... layoutTypes) {
		offsetList = new TIntArrayList();
		int runningOffset = 0;
		for(LayoutType type : layoutTypes) {
			int align = type.alignment;
			int size = type.size;
			//round up//
			runningOffset = runningOffset + (align - 1);
			runningOffset /= align;
			runningOffset *= align;
			//insert and handle size//
			offsetList.add(runningOffset);
			runningOffset += size;
		}
		size = runningOffset;
	}

	public int getTotalSize() {
		return size;
	}

	public int getOffset(int index) {
		return offsetList.get(index);
	}

}
