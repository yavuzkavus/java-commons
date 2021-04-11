package com.readjournal.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.readjournal.exception.NestedException;
import com.readjournal.util.StringUtil;

public class ThreadHouse extends ThreadPoolExecutor {
	private BiConsumer<Thread, Runnable> beforeExecute;
	private Consumer<Runnable> afterExecute;

	public ThreadHouse(	int corePoolSize,
						int maximumPoolSize,
						BiConsumer<Thread, Runnable> beforeExecute, 
						Consumer<Runnable> afterExecute) {
		this(corePoolSize, maximumPoolSize, null, beforeExecute, afterExecute);
	}

	public ThreadHouse(	int corePoolSize,
						int maximumPoolSize,
						String namePrefix,
						BiConsumer<Thread, Runnable> beforeExecute, 
						Consumer<Runnable> afterExecute) {
		super(	corePoolSize,
				maximumPoolSize,
				60L,
				TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(),
				new ThreadHouseFactory( StringUtil.ifNull(namePrefix, "ThreadHouse") ) );
		this.beforeExecute = beforeExecute;
		this.afterExecute = afterExecute;
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if( beforeExecute!=null )
			beforeExecute.accept(t, r);
	}
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if( afterExecute!=null )
			afterExecute.accept(r);
	}

	public void submit(Runnable runnable, int timeout, TimeUnit unit) {
		Future<Object> future = null;
		try {
			future = submit(runnable, null);
			future.get(timeout, unit);
		}
		catch(TimeoutException ex) {
			if( future!=null ) {
				try { future.cancel(true); } catch(Exception ex2) {}
			}
			throw NestedException.wrap(ex);
		}
		catch(Exception ex) {
			throw NestedException.wrap(ex);
		}
	}

	public <T> T submit(Callable<T> callable, int timeout, TimeUnit unit) {
		Future<T> future = null;
		try {
			future = submit(callable);
			return future.get(timeout, unit);
		}
		catch(TimeoutException ex) {
			if( future!=null ) {
				try { future.cancel(true); } catch(Exception ex2) {}
			}
			throw NestedException.wrap(ex);
		}
		catch(Exception ex) {
			throw NestedException.wrap(ex);
		}
	}
	
	static class ThreadHouseFactory implements ThreadFactory {
		private final AtomicLong threadCounter = new AtomicLong();
		private final ThreadFactory executorsDefaultThreadFactory = Executors.defaultThreadFactory();
		private String namePrefix;
		
		public ThreadHouseFactory(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = executorsDefaultThreadFactory.newThread(r);
			thread.setPriority(Thread.NORM_PRIORITY);
			thread.setDaemon(true);
			thread.setName(String.format("%s-%04d", namePrefix, threadCounter.incrementAndGet()));
			return thread;
		}
	}
}
