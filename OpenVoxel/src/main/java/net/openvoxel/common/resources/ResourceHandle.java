package net.openvoxel.common.resources;

import com.jc.util.format.json.JSON;
import com.jc.util.format.json.JSONObject;

/**
 * Created by James on 25/08/2016.
 *
 * A Resource Pack Volatile Resource
 */
public class ResourceHandle {

	private ResourceType type;
	private String ID;

	private boolean reload;
	private byte[] Data;

	public ResourceHandle(ResourceType type, String ID) {
		this.type = type;
		this.ID = ID;
		markAsDirty();
	}

	public void markAsDirty() {
		reload = true;
	}

	/**
	 *
	 * @return If The Resource Should Be Reloaded
	 */
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

	public void reloadData() {
		Data = ResourceDataHandler.getData(getID());
		reload = false;
	}

	public void unloadData() {
		Data = null;
	}

	public byte[] getByteData() {
		if(Data == null) {
			reloadData();
		}
		return Data;
	}

	public String getStringData() {
		return new String(getByteData());
	}

	public String getResourceID() {
		return ID;
	}

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
