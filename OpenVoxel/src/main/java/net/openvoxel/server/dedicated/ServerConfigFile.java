package net.openvoxel.server.dedicated;

import com.jc.util.format.json.JSON;
import com.jc.util.format.json.JSONInteger;
import com.jc.util.format.json.JSONObject;
import com.jc.util.format.json.JSONString;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by James on 04/09/2016.
 *
 * TODO: FINISH IMPLEMENTATION
 *
 * Configuration File for the server
 */
public class ServerConfigFile {

	public static ServerConfigFile instance;
	public static void Load() {
		if(instance != null) {
			instance = new ServerConfigFile(new File("server-properties.json"));
		}
	}

	private JSONObject json;
	private File file;
	private int oldHash;
	public ServerConfigFile(File file) {
		this.file = file;
		forceLoad();
	}

	public void forceLoad() {
		try {
			json = JSON.fromFile(file);
			oldHash = file.hashCode();
		}catch(Exception e) {
			json = null;
		}
	}
	public void load() {
		int hash = file.hashCode();
		if(hash != oldHash) {
			forceLoad();
		}
	}


	public void save() {
		try{
			FileOutputStream fout = new FileOutputStream(file);
			fout.write(json.toPrettyJSONString().getBytes());
			fout.close();
		}catch(Exception e) {}
	}

	private int _getInt(String key,int def) {
		load();
		if(json.asMap().contains(key)) {
			return json.asMap().get(key).asInteger();
		}else{
			json.asMap().put(new JSONString(key),new JSONInteger(def));
			save();
			return def;
		}
	}

	public int getPort() {
		return _getInt("port",6556);
	}



}
