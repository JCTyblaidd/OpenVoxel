package net.openvoxel.lauch.handler;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by James on 03/09/2016.
 */
public class LaunchBuilder {

	private Set<String> flags;
	private Map<String,String> maps;
	private File launchFile = null;
	private File directory = null;
	private Set<String> jvm_flags;
	private Map<String,String> jvm_maps;
	private InputStream OUT = null, ERR = null;
	private boolean KILL = false;

	public LaunchBuilder() {
		flags = new HashSet<>();
		maps = new HashMap<>();
		jvm_flags = new HashSet<>();
		jvm_maps = new HashMap<>();
	}

	public void addFlag(String flag) {
		flags.add(flag);
	}

	public void addMapping(String key,Object value) {
		maps.put(key,value.toString());
	}

	public void setLocation(File file) {
		launchFile = file;
	}

	public void addJVMFlag(String flag) {
		jvm_flags.add(flag);
	}
	public void addJVMSetting(String key, Object value) {
		jvm_maps.put(key,value.toString());
	}

	public void setStreams(InputStream OUT,InputStream ERR) {
		this.OUT = OUT;
		this.ERR = ERR;
	}

	public void setShutdownOnLaunch(boolean flag) {
		KILL = flag;
	}


	public void Launch() {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(getArgs());
		if(directory != null) {
			builder.directory(directory);
		}

		if(OUT != null) {

		}

		if(ERR != null) {

		}

		if(KILL) {
			System.exit(0);
		}
	}

	private String getArgs() {
		StringBuilder builder = new StringBuilder("java");
		//JVM Params//
		for(String flag : jvm_flags) {
			builder.append(' ');
			builder.append(flag);
		}
		for(Map.Entry<String,String> v : jvm_maps.entrySet()) {
			builder.append(' ');
			builder.append(v.getKey());
			builder.append('=');
			builder.append(v.getValue());
		}
		builder.append(" -jar");
		//File//
		builder.append(' ');
		builder.append(launchFile.getAbsolutePath());
		//Program Params//
		for(String flag : flags) {
			builder.append(" -");
			builder.append(flag);
		}
		for(Map.Entry<String,String> v : maps.entrySet()) {
			builder.append(' ');
			builder.append(v.getKey());
			builder.append('=');
			builder.append(v.getValue());
		}
		return builder.toString();
	}

}
