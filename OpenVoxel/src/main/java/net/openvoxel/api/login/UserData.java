package net.openvoxel.api.login;

import com.jc.util.utils.ArgumentParser;
import net.openvoxel.OpenVoxel;
import net.openvoxel.api.side.Side;

import java.util.UUID;

/**
 * Created by James on 03/09/2016.
 *
 * User ID Reference
 */
public class UserData {

	public final String UserName;
	public final UUID uniqueID;

	public UserData(String name, UUID uuid) {
		UserName = name;
		uniqueID = uuid;
		if(Side.isClient) {
			OpenVoxel.getLogger().Info("Loaded Client: UserName = "+UserName);
		}
	}

	public static UserData from(ArgumentParser args) {
		return new UserData(args.getStringMap("username"),uuidFromArg(args));
	}

	static UUID uuidFromArg(ArgumentParser args) {
		if(args.hasKey("uuid")) {
			try{
				return UUID.fromString(args.getStringMap("uuid"));
			}catch(Exception e) {
				return null;
			}
		}else{
			return null;
		}
	}

}
