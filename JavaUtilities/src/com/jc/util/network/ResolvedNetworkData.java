package com.jc.util.network;

import com.jc.util.format.json.JSON;
import com.jc.util.format.json.JSONObject;

import java.nio.charset.Charset;

/**
 * Created by James on 23/09/2016.
 *
 * Resolved Network Handler
 */
public class ResolvedNetworkData {

	private byte[] data;
	ResolvedNetworkData(byte[] data) {
		this.data = data;
	}

	public byte[] getBytes() {
		return data;
	}

	public String getString() {
		return new String(data);
	}

	public String getString(Charset charset) {
		return new String(data,charset);
	}

	public JSONObject getJSON() {
		return JSON.fromString(getString());
	}
}
