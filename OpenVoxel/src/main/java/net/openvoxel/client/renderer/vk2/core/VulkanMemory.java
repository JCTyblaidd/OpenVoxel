package net.openvoxel.client.renderer.vk2.core;

import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.LongBuffer;

final class VulkanMemory implements Closeable {

	private final VulkanDevice device;

	/////////////////////////
	/// Memory Management ///
	/////////////////////////

	public long UserInterfaceTriangleStream;
	public long UserInterfaceImageStream;

	public long EntityDrawStream;
	public long WorldAsyncUpdateStream;

	//Number of Memory Pages that exist
	private long MemoryPageCount = 0;
	//ID of Memory Page at index
	private LongBuffer MemoryPages;
	//Usage info of Memory Page: 0=FREE, -1=USED_BY_PREVIOUS, >0=MEMORY_USED_OF_LENGTH
	private LongBuffer MemorySubPages;

	private final long LengthMemoryPage;
	private final long LengthMemorySubPage;
	private final long LengthAsyncUpdate;
	private final long LengthEntityDraw;
	private final long LengthInterfaceTri;
	private final long LengthInterfaceImg;

	//////////////////////
	/// Initialization ///
	//////////////////////

	VulkanMemory(VulkanDevice device) {
		this.device = device;
		LengthMemoryPage = 0;
		LengthMemorySubPage = 0;
		LengthAsyncUpdate = 0;
		LengthEntityDraw = 0;
		LengthInterfaceTri = 0;
		LengthInterfaceImg = 0;
	}

	@Override
	public void close() {
		//Cleanup//
		if(MemoryPages != null) {
			MemoryUtil.memFree(MemoryPages);
		}
		if(MemorySubPages != null) {
			MemoryUtil.memFree(MemoryPages);
		}
	}

	///////////////////////////
	/// Interface Functions ///
	///////////////////////////

	//Return index of memory sub-page acquired, -1 for failure
	public int acquirePagedMemory(long length) {
		return 0;
	}

	//Frees paged memory
	public void releasePagedMemory(int ID) {

	}

	public boolean canPagedMemoryBeReclaimed() {
		return false;
	}

	public void reclaimPagedMemory() {

	}

	//Get next memory index that can be de-fragmented, -1 for none
	//Invalidates on acquire release
	public int getNextValidDefragIndex() {
		return -1;
	}

	public int getDefragTarget() {
		return 0;
	}

	//Perform Internal De-fragmenting
	public void performDefrag(int src, int dst) {

	}

	/////////////////////
	/// Memory Paging ///
	/////////////////////

	private void addPage() {

	}

	private void removeLastPage() {

	}
}
