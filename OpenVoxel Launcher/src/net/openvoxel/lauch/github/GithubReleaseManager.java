package net.openvoxel.lauch.github;

import com.jc.util.format.json.JSONMap;
import com.jc.util.format.json.JSONObject;
import com.jc.util.network.NetworkConnection;
import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 23/09/2016.
 *
 * Integrate with Github and use the release api
 */
public class GithubReleaseManager {

	private static final String userName = "JCTyBlaidd";
	private static final String repoName = "OpenVoxel";
	private static final String listReleaseDir = "https://api.github.com/repos/"+userName+"/"+repoName+"/releases";

	public static void main(String[] testing) throws Exception{
		listReleases();
	}

	private static JSONObject jsonObject = null;
	private static List<String> dataObject = new ArrayList<>();
	public static List<String> listReleases() throws Exception{
		List<String> releases = new ArrayList<>();
		JSONObject json = NetworkConnection.connect(listReleaseDir).get().getJSON();
		jsonObject = json;
		for(JSONObject object : json.asList()) {
			releases.add(object.asMap().get("name").asString());
		}
		dataObject.clear();
		dataObject.addAll(releases);
		return releases;
	}

	public static byte[] downloadRelease(String release) {
		if(dataObject.contains(release)) {
			int index = dataObject.indexOf(release);
			JSONMap MAP = jsonObject.asList().get(index).asMap();
			//TODO: read download URL from map
			String dwnloadURL = null;
			return NetworkConnection.connect(dwnloadURL).download().getBytes();
		}
		return null;
	}

}
