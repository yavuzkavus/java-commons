package com.readjournal.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;

public class ArrayUtil {	
	public static boolean contains(final short elementToFind, final short... array) {
		return ArrayUtils.indexOf(array, elementToFind)>=0;
	}	
	public static boolean contains(final int elementToFind, final int... array) {
		return ArrayUtils.indexOf(array, elementToFind)>=0;
	}
	
	public static boolean contains(final long elementToFind, final long... array) {
		return ArrayUtils.indexOf(array, elementToFind)>=0;
	}
	
	public static boolean contains(final String elementToFind, final String... array) {
		return ArrayUtils.indexOf(array, elementToFind)>=0;
	}
	
	public static boolean contains(final Object elementToFind, final Object... array) {
		return ArrayUtils.indexOf(array, elementToFind)>=0;
	}
	
	public static <T> T[] remove(final T array[], int index) {
		int len = array.length;
		if(index<0 || index>=len) 
			throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + len);

		@SuppressWarnings("unchecked")
		T result[] = (T[])Array.newInstance(array.getClass().getComponentType(), len - 1);
		System.arraycopy(array, 0, result, 0, index);
		if( index<len - 1 ) 
			System.arraycopy(array, index + 1, result, index, len - index - 1);

		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] concatArrays(Class<T> clazz, T[]... arrays) {
		int len = 0;
		for( T[] array : arrays ) {
			len += array.length;
		}

		T[] newArray = (T[])Array.newInstance(clazz, len);
		int i = 0;

		for( T[] array : arrays ) {
			for(T t : array ) {
				newArray[i++] = t;
			}
		}
		return newArray;
	}

	public static void ascendArray(Object[] list, String field) {
		if( list==null || list.length<=1 )
			return;
		Class<?> clas = list[0].getClass();
		try {
			final Method method = clas.getMethod(field);
			Comparator<Object> comparator = new Comparator<Object>() {
				@Override public int compare(Object o1, Object o2) {
					return Utils.compare(o1, o2, method);
				}
			};
			Arrays.sort(list, comparator);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static void descendArray(Object[] list, String field) {
		if( list==null || list.length<=1 )
			return;
		Class<?> clas = list[0].getClass();
		try {
			final Method method = clas.getMethod(field);
			Comparator<Object> comparator = new Comparator<Object>() {
				@Override public int compare(Object o1, Object o2) {
					return Utils.compare(o2, o1, method);
				}
			};
			Arrays.sort(list, comparator);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
}
