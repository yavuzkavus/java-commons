package com.readjournal.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtil {
	@SafeVarargs
	public static <T> Set<T> asSet(T... els) {
		Set<T> set = new HashSet<>(els.length);
		for(T el : els)
			set.add(el);
		return set;
	}

	public static <T> List<T> propertyList(Object[] collect, String methodName) {
		return propertyList(Arrays.asList(collect), methodName);
	}

	public static <T> List<T> propertyList(Collection<?> collect, String methodName) {
		if( collect==null || collect.isEmpty() )
			return Collections.emptyList();
		try {
			Class<?> clas = collect.iterator().next().getClass();
			Method method = clas.getMethod(methodName);
			List<T> list = new ArrayList<T>( collect.size() );
			for( Object obj : collect ) {
				T ret = Utils.cast(method.invoke(obj));
				list.add( ret );
			}
			return list;
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static <T> Set<T> propertySet(Object[] collect, String methodName) {
		return propertySet(Arrays.asList(collect), methodName);
	}

	public static <T> Set<T> propertySet(Collection<?> collect, String methodName) {
		if( collect==null || collect.isEmpty() )
			return Collections.emptySet();
		try {
			Class<?> clas = collect.iterator().next().getClass();
			Method method = clas.getMethod(methodName);
			Set<T> set = new HashSet<T>( collect.size() );
			for( Object obj : collect ) {
				T ret = Utils.cast(method.invoke(obj));
				set.add( ret );
			}
			return set;
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static <K, V> Map<K, V> propertyMap(Collection<V> collect, String methodName) {
		if( collect == null || collect.isEmpty() )
			return Collections.emptyMap();
		try {
			Class<?> clas = collect.iterator().next().getClass();
			Method method = clas.getMethod(methodName);
			Map<K, V> map = new HashMap<K, V>(collect.size());
			for (V obj : collect) {
				K ret = Utils.cast(method.invoke(obj));
				map.put(ret, obj);
			}
			return map;
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static <T> T[] propertyArray(Object[] collect, String methodName) {
		return propertyArray(Arrays.asList(collect), methodName);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] propertyArray(Collection<?> collect, String methodName) {
		if( collect==null || collect.isEmpty() )
			return (T[])new Object[0];
		try {
			Class<?> clas = collect.iterator().next().getClass();
			Method method = clas.getMethod(methodName, (Class<?>[])null);
			Class<T> retClas = (Class<T>)method.getReturnType();
			if( retClas==int.class )
				retClas = (Class<T>)Integer.class;
			else if( retClas==long.class )
				retClas = (Class<T>)Long.class;
			T[] arr = (T[])Array.newInstance(retClas, collect.size());
			int i = 0;
			for( Object obj : collect ) {
				T ret = Utils.cast(method.invoke(obj));
				arr[i] = ret;
				i++;
			}
			return arr;
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static void ascendList(List<?> list, String field) {
		if( list==null || list.size()<=1 )
			return;
		Class<?> clas = list.get(0).getClass();
		try {
			final Method method = clas.getMethod(field);
			Comparator<Object> comparator = new Comparator<Object>() {
				@Override public int compare(Object o1, Object o2) {
					return Utils.compare(o1, o2, method);
				}
			};
			Collections.sort(list, comparator);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static void descendList(List<?> list, String field) {
		if( list==null || list.size()<=0 )
			return;
		Class<?> clas = list.get(0).getClass();
		try {
			final Method method = clas.getMethod(field);
			Comparator<Object> comparator = new Comparator<Object>() {
				@Override public int compare(Object o1, Object o2) {
					return Utils.compare(o2, o1, method);
				}
			};
			Collections.sort(list, comparator);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> toMap(Object... values) {
		Map<K, V> map = new HashMap<>(values.length/2);
		for(int i=0; i<values.length/2; i++) {
			map.put((K)values[2*i], (V)values[2*i+1]);
		}
		return map;
	}

	public static <K, V> Map<K, V> toUnmodifiableMap(Object... values) {
		return Collections.unmodifiableMap(toMap(values));
	}

	public static <T> List<T> toList(Collection<T> collect) {
		if( collect instanceof List<?> )
			return (List<T>)collect;
		return new ArrayList<T>(collect);
	}
}
