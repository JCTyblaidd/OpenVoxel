package com.jc.util.config;

/**
 * Created by James on 05/08/2016.
 */
public enum ConfigType {
	JSON("json"),
	JSON_BINARY("binjson"),
	XML("xml"),
	XML_BINARY("binxml"),
	YAML("yaml"),
	YAML_BINARY("binyaml"),
	INI("ini"),
	INI_BINARY("binini");

	private String extension;

	ConfigType(String s) {
		extension = s;
	}
	public String getExtension() {
		return extension;
	}
}
