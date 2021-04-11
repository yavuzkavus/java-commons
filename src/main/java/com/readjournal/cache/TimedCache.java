package com.readjournal.cache;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TimedCache<T> {
	private final long timeoutMillis;
	private final Supplier<T> supplier;
	private final Executor executor;
	
	private volatile boolean loading;
	private volatile long lastLoadMillis;
	private volatile T data;
	
	private TimedCache(Supplier<T> supplier, long timeoutMillis, Executor executor) {
		Objects.requireNonNull(supplier);
		if( timeoutMillis<=0 )
			new IllegalArgumentException("timeoutMillis should be positive");
		this.supplier = supplier;
		this.timeoutMillis = timeoutMillis;
		this.executor = executor;
	}
	
	private boolean shouldLoad() {
		long passedTime = System.currentTimeMillis()-lastLoadMillis;
		return data==null || (passedTime>=timeoutMillis);
	}
	
	private void load() {
		try {
			data = supplier.get();
			lastLoadMillis = System.currentTimeMillis();
		}
		finally {
			loading = false;
		}
	}
	
	public void reload() {
		lastLoadMillis = 0;
	}
	
	public T get() {
		if( shouldLoad() ) {
			synchronized(this) {
				if( shouldLoad() ) {
					if( data==null || executor==null ) {
						loading = true;
						load();
					}
					else if( !loading ) {
						loading = true;
						executor.execute(this::load);
					}
				}
			}
		}
		return data;
	}
	  
	public static <T> Builder<T> newBuilder() {
		return new Builder<T>();
	}
	
	public static class Builder<T> {
		private long timeoutMillis;
		private Supplier<T> supplier;
		private Executor executor;
		
		public Builder<T> timeout(int timeout, TimeUnit unit) {
			this.timeoutMillis = unit.toMillis(timeout);
			return this;
		}
		
		public Builder<T> supplier(Supplier<T> supplier) {
			this.supplier = supplier;
			return this;
		}
		
		public Builder<T> executor(Executor executor) {
			this.executor = executor;
			return this;
		}
		
		public TimedCache<T> build() {
			return new TimedCache<T>(supplier, timeoutMillis, executor);
		}
	}
}
