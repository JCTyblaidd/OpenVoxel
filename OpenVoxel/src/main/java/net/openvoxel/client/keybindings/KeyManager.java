package net.openvoxel.client.keybindings;

import com.jc.util.format.json.*;
import net.openvoxel.files.FolderUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

/**
 * Created by James on 10/09/2016.
 *
 * Default Key Bindings: For Standard Game State, [GUI Inputs should be handled on a Per GUI state]
 *
 * Contains Automatic Mapping to Config File
 *
 */
public class KeyManager {

	private static HashMap<String,KeyBinding> bindings;
	private static File configFile;
	static {
		configFile = new File(FolderUtils.ConfigDir,"key_mappings.json");
		bindings = new HashMap<>();
		loadID();
	}

	@SuppressWarnings("unchecked")
	private static void loadID() {
		try {
			JSONObject json = JSON.fromFile(configFile);
			JSONMap<JSONInteger> data = (JSONMap<JSONInteger>)json;
			data.forEach((str,id) -> {
				bindings.put(str.asString(),new KeyBinding(id.asInteger()));
			});
		}catch(Exception e) {}
	}

	static void saveID() {
		JSONMap<JSONInteger> Data = new JSONMap<>();
		bindings.forEach((id,val) -> {
			Data.put(new JSONString(id),new JSONInteger(val.KEY));
		});
		try{
			FileOutputStream file_out = new FileOutputStream(configFile);
			file_out.write(Data.toPrettyJSONString().getBytes());
			file_out.close();
		}catch(Exception e) {}
	}

	public static KeyBinding getBinding(String ID,int defaultKey) {
		KeyBinding bind = bindings.get(ID);
		if(bind == null) {
			bind = new KeyBinding(defaultKey);
			bindings.put(ID,bind);
			saveID();
		}
		return bind;
	}

}