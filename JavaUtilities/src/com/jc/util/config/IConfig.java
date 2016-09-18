package com.jc.util.config;

public interface IConfig extends IConfigEntry {
	
	void Save();
	void Reload();

	void Delete();

	@Override
	default IConfig getConfig() {
		return this;
	}
}
