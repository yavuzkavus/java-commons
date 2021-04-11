package com.readjournal.util;

import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {
	private static final Log global = new Log(Logger.getGlobal());
	private final Logger logger;
	
	private Log(Logger logger) {
		this.logger = logger;
	}
	
	public String getName() {
		return logger.getName();
	}
	
	public Level getLevel() {
		return logger.getLevel();
	}
	
	public void setLevel(Level level) {
		logger.setLevel(level);
	}
	
	public Filter getFilter() {
		return logger.getFilter();
	}
	
	public void setFilter(Filter filter) {
		logger.setFilter(filter);
	}
	
	public void addHandler(Handler handler) {
		logger.addHandler(handler);
	}
	
	public void removeHandler(Handler handler) {
		logger.removeHandler(handler);
	}
	
	public Handler[] getHandlers() {
		return logger.getHandlers();
	}
	
	public Log getParent() {
		return logger.getParent()==null ? null : new Log(logger.getParent());
	}
	
	public void info(String msg) {
		logger.info(msg);
	}
	
	public void severe(String msg) {
		logger.severe(msg);
	}
	
	public void severe(Throwable thr) {
		logger.log(Level.SEVERE, thr.getMessage(), thr);
	}
	
	public void severe(String msg, Throwable thr) {
		logger.log(Level.SEVERE, msg, thr);
	}
	
	public void warning(String msg) {
		logger.warning(msg);
	}
	
	public void warning(Throwable thr) {
		logger.log(Level.WARNING, thr.getMessage(), thr);
	}
	
	public void warning(String msg, Throwable thr) {
		logger.log(Level.WARNING, msg, thr);
	}
	
	@Override
	public String toString() {
		return logger.toString();
	}
	
	public static Log getLog() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		String name = stackTraceElements[stackTraceElements.length-1].getClassName();
		for(int i=1; i<stackTraceElements.length-1; i++) {
			if( !stackTraceElements[i].getClassName().equals(Log.class.getName()) ) {
				name = stackTraceElements[i].getClassName();
				break;
			}
		}
		return new Log(Logger.getLogger(name));
	}
	
	public static Log getLog(Class<?> clas) {
		return new Log(Logger.getLogger(clas.getName()));
	}
	
	public static Log getLog(String name) {
		return new Log(Logger.getLogger(name));
	}
	
	public static Log getGlobal() {
		return global;
	}
}
