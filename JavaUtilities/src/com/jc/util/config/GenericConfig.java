package com.jc.util.config;

import com.jc.util.config.json.JSONConfig;
import com.jc.util.config.util.IRootedConfig;

import java.io.File;

/**
 * Created by James on 05/08/2016.
 */
public class GenericConfig implements IRootedConfig {

	private ConfigType type;
	private IConfig config_handle;

	public GenericConfig(String str) {
		this(new File(str));
	}
	public GenericConfig(File f) {
		_initSmartLoad(f);
	}
	public GenericConfig(String str, ConfigType type) {
		this(new File(str),type);
	}
	public GenericConfig(File f, ConfigType type) {
		_initLoad(f,type);
	}

	private void _initLoad(File f, ConfigType t) {
		if(t == ConfigType.JSON) {
			config_handle = new JSONConfig(f);
		}else
		if(t == ConfigType.JSON_BINARY) {
			config_handle = new JSONConfig(f,false,false);//Binay//
		}
	}
	private void _initSmartLoad(File f) {
		String[] v2 = f.getAbsolutePath().split(".");
		String type = v2[v2.length-1];
		switch (type) {
			case "json":
				_initLoad(f,ConfigType.JSON);
				return;
			case "bin-json":
				_initLoad(f,ConfigType.JSON_BINARY);
				return;
			case "xml":
				_initLoad(f,ConfigType.XML);
				return;
			case "bin-xml":
				_initLoad(f,ConfigType.XML_BINARY);
				return;
			case "yaml":
				_initLoad(f,ConfigType.YAML);
				return;
			case "bin-yaml":
				_initLoad(f,ConfigType.YAML_BINARY);
				return;
			case "ini":
				_initLoad(f,ConfigType.INI);
				return;
			case "bin-ini":
				_initLoad(f,ConfigType.INI_BINARY);
				return;
		}
		_initSmartLoad(f);
	}

	private void _initGuessLoad(File f) {
		//Try Everything Until Something Works!!!///
		throw new RuntimeException("Not Yet Implemented: Trial and Erro Config Loading");
	}

	public ConfigType getType() {
		return type;
	}


	public Object getSubSection(String index) {
		return null;
	}

	@Override
	public IConfigEntry getRoot() {
		return config_handle;
	}

	@Override
	public void Save() {
		config_handle.Save();
	}

	@Override
	public void Reload() {
		config_handle.Reload();
	}

	@Override
	public void Delete() {
		config_handle.Delete();
	}

	@Override
	public IConfig getConfig() {
		return config_handle;
	}
}
