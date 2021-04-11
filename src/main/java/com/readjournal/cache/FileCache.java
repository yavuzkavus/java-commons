package com.readjournal.cache;

import java.io.File;
import java.util.Objects;

import com.readjournal.util.FileUtil;

public class FileCache {
	private final long checkIntervalMillis;
	private final File file;
	
	private volatile long lasCheckMillis;
	private volatile long lastModified;
	private volatile String data;
	
	private FileCache(File file, long checkIntervalMillis) {
		Objects.requireNonNull(file);
		if( !file.exists() )
			throw new RuntimeException("File not found: " + file.getAbsolutePath());
		this.file = file;
		if( checkIntervalMillis<=0 )
			new IllegalArgumentException("timeoutMillis should be positive");
		this.checkIntervalMillis = checkIntervalMillis;
	}
	
	private boolean shouldLoad() {
		long passedTime = System.currentTimeMillis()-lasCheckMillis;
		return data==null || (passedTime>=checkIntervalMillis);
	}
	
	public void reload() {
		lasCheckMillis = 0;
	}
	
	public String get() {
		if( shouldLoad() ) {
			synchronized(this) {
				if( shouldLoad() ) {
					long currentLastModified = file.lastModified();
					if( data==null || lastModified<currentLastModified ) {
						data = FileUtil.readFileToString(file);
					}
					lastModified = currentLastModified;
					lasCheckMillis = System.currentTimeMillis();
				}
			}
		}
		return data;
	}
}
