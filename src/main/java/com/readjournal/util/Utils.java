package com.readjournal.util;

import java.io.File;
import java.lang.reflect.Method;

import com.readjournal.exception.NestedException;

public class Utils {
	public static RuntimeException runtime(Throwable thr) {
		return NestedException.wrap(thr);
	}

	public static RuntimeException runtime(String thr) {
		return new RuntimeException(thr);
	}

	public static boolean equals(Object obj1, Object obj2){
		if( obj1==null && obj2==null )
			return true;
		return obj1!= null && obj1.equals(obj2);
	}

	public static boolean equals(float d1, float d2, float tolerance) {
		return Math.abs(d1-d2)<=tolerance;
	}

	@SuppressWarnings("unchecked")
	public static int compare(Object o1, Object o2, Method method) {
		try {
			Comparable<Object> comparable1 = (Comparable<Object>)method.invoke(o1);
			Comparable<Object> comparable2 = (Comparable<Object>)method.invoke(o2);
			return compare(comparable1, comparable2);
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compare(Comparable obj1, Comparable obj2){
		if( obj1==null && obj2==null )
			return 0;

		if( obj1 instanceof String || obj2 instanceof String ) {
			if( obj1 == null )
				obj1 = "";
			if( obj2 == null )
				obj2 = "";
			return StringUtil.compareTurkish((String)obj1, (String)obj2);
		}

		if( obj1==null && obj2!=null )
			return 1;
		else if( obj2==null )
			return -1;
		return obj1.compareTo(obj2);
	}

	public static int toInt(String str, int def) {
		try {
			return Integer.parseInt(str);
		}
		catch (Exception e) {
			return def;
		}
	}

	public static int toInt(String str) {
		return toInt(str, 0);
	}

	public static int toInt(String str, int radix, int def) {
		try {
			return Integer.parseInt(str, radix);
		}
		catch (Exception e) {
			return def;
		}
	}

	public static long toLong(String str, long def) {
		try {
			return Long.parseLong(str);
		}
		catch (Exception e) {
			return def;
		}
	}

	public static long toLong(String str) {
		return toLong(str, 0);
	}

	public static float toFloat(String str, float def) {
		try {
			return Float.parseFloat(str);
		}
		catch (Exception e) {
			return def;
		}
	}

	public static float toFloat(String str) {
		return toFloat(str, 0);
	}

	public static double toDouble(String str, double def) {
		try {
			return Double.parseDouble(str);
		}
		catch (Exception e) {
			return def;
		}
	}

	public static double toDouble(String str) {
		return toDouble(str, 0);
	}

	public static boolean toBoolean(String str, boolean def) {
		try {
			return Boolean.parseBoolean(str);
		}
		catch (Exception e) {
			return def;
		}
	}

	public static boolean toBoolean(String str) {
		return toBoolean(str, false);
	}
	
	public static boolean deleteSilently(File file) {
		if( file!=null ) {
			try {
				return file.delete();
			}
			catch(Exception ex) { }
		}
		return false;
	}

	public static void closeSilently(Process process) {
		if( process!=null ) {
			try { process.destroy(); } catch(Exception ex) { }
		}
	}

	public static void closeSilently(AutoCloseable... arr) {
		for( AutoCloseable c : arr ) {
			try { c.close(); } catch(Exception ex) { }
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj) {
		return (T)obj;
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) { }
	}
}
