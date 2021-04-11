package com.readjournal.task;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class FileDeleteCron implements Runnable {
	private final Map<String, Long> files = Collections.synchronizedMap(new HashMap<>(100));
	@Override
	public void run() {
		synchronized (files) {
			Iterator<Entry<String, Long>> iter = files.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<String, Long> entry = iter.next();
				if( entry.getValue()<=System.currentTimeMillis() ) {
					new File(entry.getKey()).delete();
					iter.remove();
				}
			}
		}
	}

	public boolean add(File file, int afterSeconds) {
		try {
			String path = file.getCanonicalPath();
			files.put(path, System.currentTimeMillis() + afterSeconds*1000);
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
}
