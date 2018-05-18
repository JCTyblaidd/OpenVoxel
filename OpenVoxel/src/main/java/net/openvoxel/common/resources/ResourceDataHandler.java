package net.openvoxel.common.resources;

import net.openvoxel.api.logger.Logger;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by James on 25/08/2016.
 */
class ResourceDataHandler {

	private static final Logger resourceLog = Logger.getLogger("Resource Loader");

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

	static ByteBuffer getAllocData(String loc) {
		try{
			InputStream stream = ResourceDataHandler.class.getClassLoader().getResourceAsStream(loc);
			if(stream == null) {
				resourceLog.Severe("Failed to load resource: "+loc);
				return null;
			}else{
				byte[] data = stream.readAllBytes();
				stream.close();
				ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
				buffer.put(data);
				buffer.position(0);
				return buffer;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
