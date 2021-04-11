package com.readjournal.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import com.readjournal.util.StringUtil;
import com.readjournal.util.Utils;

public class Config {
	private Map<String, Setting> confMap;
	//private List<Setting> defaultConf;
	private File configFile;
	
	private List<Consumer<Set<String>>> changeListeners;

	public Config(File configFile, List<Setting> defaultConf) {
		this.configFile = configFile;
		//this.defaultConf = defaultConf;
		this.confMap = new LinkedHashMap<String, Setting>(defaultConf.size());
		for(Setting setting : defaultConf)
			confMap.put(setting.getName(), setting);
		load();
		changeListeners = Collections.synchronizedList(new ArrayList<>());
	}
	
	Map<String, Setting> getConfMap() {
		return Collections.unmodifiableMap(confMap);
	}

	public void save(Map<String, String> changeds) {
		Properties props = new Properties();
		for(Entry<String, String> entry : changeds.entrySet()) {
			confMap.get(entry.getKey()).setValue(entry.getValue());
		}
		for(Setting setting: confMap.values()) {
			/*
			if( setting.getName().toUpperCase().indexOf("PASSWORD")>-1 )
				props.setProperty(setting.getName(), SecurityUtil.encryptString(setting.getValue()));
			else
			*/
			props.setProperty(setting.getName(),  StringUtil.ifNull(setting.getValue()));
		}

		try(OutputStream out = new FileOutputStream(configFile)) {
			props.store(out, Config.class.getName());
		}
		catch(IOException e) {
			throw Utils.runtime(e);
		}
		if( changeListeners.size()>0 ) {
			synchronized (changeListeners) {
				changeListeners.forEach(consumer->consumer.accept(changeds.keySet()));
			}
		}
	}

	public void save() {
		Properties props = new Properties();
		for(Setting setting: confMap.values()) {
			/*
			if( setting.getName().toUpperCase().indexOf("PASSWORD")>-1 )
				props.setProperty(setting.getName(), SecurityUtil.encryptString(setting.getValue()));
			else
			*/
			props.setProperty(setting.getName(), StringUtil.ifNull(setting.getValue()));
		}

		try(OutputStream out = new FileOutputStream(configFile)) {
			props.store(out, Config.class.getName());
		}
		catch(IOException e) {
			throw Utils.runtime(e);
		}
	}

	public void load() {
		if( configFile.exists() ) {
			try {
				Properties props = new Properties();
				try(InputStream in = new FileInputStream(configFile)) {
					props.load(in);
				}
				boolean save = false;
				for( Setting setting : confMap.values() ) {
					String value = props.getProperty( setting.getName() );
					if( value==null ) {
						save = true;
					}
					else {
						/*
						if( setting.getName().toUpperCase().indexOf("PASSWORD")>-1 )
							setting.setValue(SecurityUtil.decryptString(value));
						else
						*/
						setting.setValue( value );
					}
				}
				if( save )
					save();
			}
			catch(IOException e) {
				throw Utils.runtime(e);
			}
		}
		else {
			save();
		}
	}

	public String getStringValue(String key) {
		return confMap.get(key).getValue();
	}

	public int getIntValue(String key) {
		return Integer.parseInt( confMap.get(key).getValue() );
	}

	public long getLongValue(String key) {
		return Long.parseLong( confMap.get(key).getValue() );
	}

	public boolean getBooleanValue(String key) {
		return Boolean.parseBoolean( confMap.get(key).getValue() );
	}
	
	public boolean isRequireRestart(String key) {
		return confMap.get(key).isRequireRestart();
	}
	
	public void addChangeListener(Consumer<Set<String>> changeListener) {
		changeListeners.add(changeListener);
	}
}
