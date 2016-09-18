package net.openvoxel.common.resources;

import net.openvoxel.client.control.Renderer;

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
				return "shaders/"+this.ID+"."+Renderer.renderer.getShaderPostfix();
			case TEXT:
				return "text/"+this.ID+".txt";
		}
		throw new RuntimeException("Error Parsing Resource Type: " + type);
	}

	public void reloadData() {
		Data = ResourceDataHandler.getData(getID());
		reload = false;
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

}
