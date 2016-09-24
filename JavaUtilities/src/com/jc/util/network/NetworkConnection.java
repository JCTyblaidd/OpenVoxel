package com.jc.util.network;

import com.jc.util.stream.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by James on 23/09/2016.
 *
 * Quick Network Handler
 */
public class NetworkConnection {

	private URL url;
	private NetworkConnection(URL url) {
		this.url = url;
	}

	public static NetworkConnection connect(String str) {
		try {
			return connect(new URL(str));
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static NetworkConnection connect(URL url) {
		return new NetworkConnection(url);
	}

	public ResolvedNetworkData get() {
		try {
			byte[] res;
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			InputStream in = (InputStream) connection.getContent();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			IOUtils.copyStream(in,bout,512);
			in.close();
			res = bout.toByteArray();
			return new ResolvedNetworkData(res);
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public ResolvedNetworkData download() {
		try{
			return null;//TODO:
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
