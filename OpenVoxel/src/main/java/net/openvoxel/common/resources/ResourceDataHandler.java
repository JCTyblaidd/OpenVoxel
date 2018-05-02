package net.openvoxel.common.resources;

import net.openvoxel.api.logger.Logger;

import java.io.InputStream;

/**
 * Created by James on 25/08/2016.
 */
class ResourceDataHandler {

	static byte[] getData(String loc) {
		try {
			InputStream stream = ResourceDataHandler.class.getClassLoader().getResourceAsStream(loc);
			if(stream == null) {
				Logger.getLogger("Resource Logger").Severe("Resource @ " + loc + " doesn't exist!");
				return null;
			}else {
				byte[] data = stream.readAllBytes();
				stream.close();
				return data;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
