package com.jc.util.config.json;

import com.jc.util.config.IConfigEntry;
import com.jc.util.config.util.IRootedConfig;
import com.jc.util.filesystem.FileHandle;
import com.jc.util.format.json.JSON;
import com.jc.util.format.json.JSONObject;

import java.io.File;

/**
 * Created by James on 13/08/2016.
 */
public class JSONConfig implements IRootedConfig{

	private File ref;
	private FileHandle handle;
	private JSONObject root_object;
	private JSONConfigEntry root_entry;
	private boolean _deleted = false;
	private boolean fancy;
	private boolean text;

	public JSONConfig(File f) {
		this(f,true);
	}

	public JSONConfig(File f, boolean fancy) {
		this(f,fancy,true);
	}

	public JSONConfig(File f,boolean fancy,boolean text) {
		ref = f;
		handle = new FileHandle(ref);
		this.fancy = fancy;
		Reload();
		root_entry = new JSONConfigEntry(root_object,this);
	}

	@Override
	public IConfigEntry getRoot() {
		return root_entry;
	}

	@Override
	public void Save() {
		if(!_deleted) {
			handle.startWrite();
			if(text) {
				if (fancy) {
					handle.write(root_object.toPrettyJSONString());
				} else {
					handle.write(root_object.toJSONString());
				}
			}else{
				handle.write(root_object.toBinaryJSON());
			}
			handle.stopWrite();
		}
	}

	@Override
	public void Reload() {
		JSONObject object;
		if(text) {
			String content = handle.getString();
			object = JSON.fromString(content);
		}else {
			byte[] content = handle.getBytes();
			object = JSON.fromBinary(content);
		}
		if(root_object == null) {
			root_object = object;
		}else {
			JSON.translateData(root_object,object);//Move the data across//
		}
	}

	@Override
	public void Delete() {
		handle.delete();
		_deleted = true;
	}
}
