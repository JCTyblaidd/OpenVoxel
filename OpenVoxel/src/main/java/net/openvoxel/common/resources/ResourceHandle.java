package net.openvoxel.common.resources;

import com.jc.util.format.json.JSON;
import com.jc.util.format.json.JSONObject;
import net.openvoxel.api.PublicAPI;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Created by James on 25/08/2016.
 *
 * A Resource Pack Volatile Resource
 */
public class ResourceHandle {

	private ResourceType type;
	private String ID;

	private boolean reload;
	private ByteBuffer Data;

	ResourceHandle(ResourceType type, String ID) {
		this.type = type;
		this.ID = ID;
		markAsDirty();
	}

	@PublicAPI
	public void markAsDirty() {
		reload = true;
	}

	/**
	 *
	 * @return If The Resource Should Be Reloaded
	 */
	@PublicAPI
	public boolean checkIfChanged() {
		return reload;
	}

	private String getID() {
		switch (type) {
			case BYTES:
				return "data/"+this.ID+".bin";
			case IMAGE:
				return "textures/"+this.ID+".png";
			case SHADER:
				return "shaders/"+this.ID+"."+ "vksl";//TODO: Renderer.renderer.getShaderPostfix();
			case TEXT:
				return "text/"+this.ID+".txt";
		}
		throw new RuntimeException("Error Parsing Resource Type: " + type);
	}

	@PublicAPI
	public void reloadData() {
		unloadData();
		Data = ResourceDataHandler.getAllocData(getID());
		reload = false;
	}

	@PublicAPI
	public void unloadData() {
		if(Data != null) {
			Data.position(0);
			MemoryUtil.memFree(Data);
			Data = null;
		}
	}

	@PublicAPI
	public byte[] getByteData() {
		ByteBuffer buf = getByteBufferData();
		if(buf == null) return null;
		byte[] bytes = new byte[buf.capacity()];
		buf.position(0);
		buf.get(bytes);
		buf.position(0);
		return bytes;
	}

	@PublicAPI
	public ByteBuffer getByteBufferData() {
		if(Data == null) {
			reloadData();
		}
		if(Data != null) Data.position(0);
		return Data;
	}

	@PublicAPI
	public String getStringData() {
		return new String(getByteData());
	}

	@PublicAPI
	public String getResourceID() {
		return ID;
	}

	@PublicAPI
	public JSONObject getMetadata() {
		String realID = getID();
		int last_idx = realID.lastIndexOf('.');
		String meta_id = realID.substring(0,last_idx) + ".json";
		byte[] SubData = ResourceDataHandler.getData(meta_id);
		if(SubData == null) return null;
		String str = new String(SubData);
		return JSON.fromString(str);
	}

	@Override
	public int hashCode() {
		return ID.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ResourceHandle && ID.equals(((ResourceHandle) obj).ID);
	}
}
