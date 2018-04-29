package net.openvoxel.client.renderer.base;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import net.openvoxel.client.renderer.common.IGuiRenderer;
import net.openvoxel.common.resources.ResourceHandle;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseGuiRenderer extends IGuiRenderer {

	///Constants
	private final Matrix4f identityMatrix = new Matrix4f().identity();
	private final BaseTextRenderer textRenderer;
	protected static final int MAX_STATE_CHANGES = 1024;

	///State Changes
	protected Set<ResourceHandle> requestedHandles = new HashSet<>(MAX_STATE_CHANGES);
	protected ResourceHandle[] resourceStateList = new ResourceHandle[MAX_STATE_CHANGES];
	protected FloatBuffer matrixStateList = MemoryUtil.memAllocFloat(16 * MAX_STATE_CHANGES);
	protected ByteBuffer  useTexStateList = MemoryUtil.memAlloc(MAX_STATE_CHANGES);
	protected IntBuffer  scissorStateList = MemoryUtil.memAllocInt(MAX_STATE_CHANGES);
	protected IntBuffer   offsetStateList = MemoryUtil.memAllocInt(MAX_STATE_CHANGES);
	protected int stateIndex = 0;
	protected int writeIndex = 0;

	//Temporary Variable Storage
	private boolean stateUsed = false;
	private final Matrix4f lastMatrix = new Matrix4f();
	private final TIntStack scissorStack = new TIntArrayStack();
	private int screenWidth;
	private int screenHeight;
	private int currScissorX, currScissorY, currScissorW, currScissorH;

	//////////////////////////
	// Abstract API Methods //
	//////////////////////////

	protected abstract BaseTextRenderer loadTextRenderer();

	protected abstract void preDraw();

	protected abstract void store(int offset,float x, float y, float u, float v, int RGB);

	protected abstract void redrawOld();

	protected abstract void createNewDraw();

	public abstract boolean allowDrawCaching();

	////////////////////////////
	/// State Change Methods ///
	////////////////////////////

	public BaseGuiRenderer() {
		textRenderer = loadTextRenderer();
	}

	public void close() {
		MemoryUtil.memFree(matrixStateList);
		MemoryUtil.memFree(useTexStateList);
		MemoryUtil.memFree(scissorStateList);
		MemoryUtil.memFree(offsetStateList);
	}

	private void advanceState() {
		resourceStateList[stateIndex + 1] = resourceStateList[stateIndex];
		int matrixIndex = stateIndex * 16;
		int nextMtIndex = matrixIndex + 16;
		for(int i = 0; i < 16; i++) {
			matrixStateList.put(nextMtIndex + i,matrixStateList.get(matrixIndex + i));
		}
		int scissorIndex = stateIndex * 4;
		int nextScsIndex = scissorIndex + 4;
		for(int i = 0; i < 4; i++) {
			scissorStateList.put(nextScsIndex + i,scissorStateList.get(scissorIndex + i));
		}
		useTexStateList.put(stateIndex + 1,useTexStateList.get(stateIndex));
		stateIndex += 1;
		offsetStateList.put(stateIndex,writeIndex);
	}

	////////////////////
	/// Drawing Code ///
	////////////////////

	public void StartDraw(int screenWidth,int screenHeight) {
		requestedHandles.clear();
		stateIndex = 0;
		stateUsed = false;
		lastMatrix.identity();
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		//Reset first state to default values
		resourceStateList[0] = null;
		matrixStateList.position(0);
		identityMatrix.get(matrixStateList);
		useTexStateList.put(0,(byte)0);
		scissorStateList.put(0,0);
		scissorStateList.put(1,0);
		scissorStateList.put(2,screenWidth);
		scissorStateList.put(3,screenHeight);
		//Reset scissor stack
		scissorStack.clear();
		currScissorX = 0;
		currScissorY = 0;
		currScissorW = screenWidth;
		currScissorH = screenHeight;
		//Reset data output
		writeIndex = 0;
		offsetStateList.put(0,0);
		preDraw();
	}

	public void finishDraw(boolean isGuiDirty) {
		if(isGuiDirty) {
			createNewDraw();
		}else{
			redrawOld();
		}
	}

	@Override
	public final void Begin() {
		EnableTexture(false);
		SetMatrix(identityMatrix);
	}

	@Override
	public final void Draw() {
		//NO OP TODO: REMOVE THIS FUNCTION?!?!?!?
	}

	@Override
	public final void SetTexture(ResourceHandle handle) {
		requestedHandles.add(handle);
		if(stateUsed && resourceStateList[stateIndex] != handle && resourceStateList[stateIndex] != null) {
			advanceState();
		}
		resourceStateList[stateIndex] = handle;
	}

	@Override
	public final void EnableTexture(boolean enabled) {
		byte enable_val = (byte)(enabled ? 1 : 0);
		if(stateUsed && useTexStateList.get(stateIndex) != enable_val) {
			advanceState();
		}
		useTexStateList.put(stateIndex,enable_val);
	}

	@Override
	public final void SetMatrix(Matrix4f mat) {
		if(stateUsed && mat.equals(lastMatrix)) {
			advanceState();
		}
		mat.get(stateIndex * 16,matrixStateList);
		lastMatrix.set(mat);
	}

	private void SetScissor(int x, int y, int w, int h) {
		if(stateUsed) {//TODO: check for non-state changes
			advanceState();
		}
		int offset = stateIndex * 4;
		scissorStateList.put(offset,x);
		scissorStateList.put(offset + 1, y);
		scissorStateList.put(offset + 2, w);
		scissorStateList.put(offset + 3, h);
	}

	@Override
	public final void VertexWithColUV(float x, float y, float u, float v, int RGB) {
		stateUsed = true;
		store(writeIndex,x,y,u,v,RGB);
		writeIndex += 1;
	}

	@Override
	public final void DrawText(float x, float y, float height, String text, int col) {
		textRenderer.DrawText(this,x,y,height,text,col);
	}

	@Override
	public final float GetTextWidthRatio(String text) {
		return textRenderer.GetTextWidthRatio(text);
	}

	@Override
	public final float getScreenWidth() {
		return screenWidth;
	}

	@Override
	public final float getScreenHeight() {
		return screenHeight;
	}

	@Override
	public final void pushScissor(int x, int y, int w, int h) {
		SetScissor(x,y,w,h);
		scissorStack.push(currScissorX);
		scissorStack.push(currScissorY);
		scissorStack.push(currScissorW);
		scissorStack.push(currScissorH);
		currScissorX = x;
		currScissorY = y;
		currScissorW = w;
		currScissorH = h;
	}

	@Override
	public final void popScissor() {
		currScissorX = scissorStack.pop();
		currScissorY = scissorStack.pop();
		currScissorW = scissorStack.pop();
		currScissorH = scissorStack.pop();
		SetScissor(
				currScissorX,
				currScissorY,
				currScissorW,
				currScissorH);
	}
}
