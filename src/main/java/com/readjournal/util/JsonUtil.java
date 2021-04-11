package com.readjournal.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @version 1.1
 *
 */
public class JsonUtil {
	private static final char[] hexChar = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };

	public static Literal buildObject(Object... args) {
		if( args.length%2!=0 )
			throw new IndexOutOfBoundsException("Element count should be two folds");
		if( args.length==0 )
			return new Literal().beginObject().endObject();
		Literal lit = new Literal().beginObject();
		for( int i=0; i<args.length; ) {
			Object temp = args[i++];
			String key = temp==null ? "" : temp.toString();
			temp = args[i++];
			lit.append(key, temp);
		}
		return lit.endObject();
	}

	public static Literal buildObject(Map<?, ?> map) {
		Literal lit = new Literal().beginObject();
		for( Entry<?, ?> entry : map.entrySet()  ) {
			lit.append(entry.getKey().toString(), entry.getValue());
		}
		return lit.endObject();
	}

	public static Literal buildArray(int[] args) {
		return buildArray(args, 0, args.length);
	}

	public static Literal buildArray(Literal[] args, int begin, int len) {
		Literal lit = new Literal().beginArray();
		for( int i=begin; i<begin+len; i++ ) {
			lit.append( args[i] );
		}
		return lit.endArray();
	}

	public static Literal buildArray(Literal[] args) {
		return buildArray(args, 0, args.length);
	}

	public static Literal buildArray(int[] args, int begin, int len) {
		Literal lit = new Literal().beginArray();
		for( int i=begin; i<begin+len; i++ ) {
			lit.append( args[i] );
		}
		return lit.endArray();
	}

	public static Literal buildArray(Object... args) {
		Literal lit = new Literal().beginArray();
		if( args.length==1 || args[0].getClass().isArray() ) {
			for(int i = 0, len = Array.getLength(args[0]); i < len; i++)
				lit.append( Array.get(args[0], i) );
		}
		else {
			for( Object arg : args )
				lit.append( arg );
		}
		return lit.endArray();
	}

	public static Literal buildArray(Collection<?> collec) {
		Literal lit = new Literal().beginArray();
		Iterator<?> iter = collec.iterator();
		while( iter.hasNext() ) {
			lit.append( iter.next() );
		}
		return lit.endArray();
	}

	public static String unicodeEscape(String s) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ((c >> 7) > 0) {
				sb.append("\\u");
				sb.append(hexChar[(c >> 12) & 0xF]); // append the hex character for the left-most 4-bits
				sb.append(hexChar[(c >> 8) & 0xF]);  // hex for the second group of 4-bits from the left
				sb.append(hexChar[(c >> 4) & 0xF]);  // hex for the third group
				sb.append(hexChar[c & 0xF]);		 // hex for the last group, e.g., the right most 4-bits
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	private static Literal prefixJson(Literal json, String prefix) {
		if( prefix==null || prefix.length()==0 )
			return json;
		StringBuilder val = json.val;
		int prefixLen = prefix.length();
		for(int i=0; i<val.length(); i++) {
			if( val.charAt(i)=='\n' ) {
				val.insert(i+1, prefix);
				i += prefixLen;
			}
		}
		return json;
	}
	
	private static String escapeJson(String str) {
		if(str==null)
			return "";
		StringBuilder sb = new StringBuilder( (int)(1.3*str.length()) );
		int code;
		for(int i=0; i<str.length(); i++) {
			code = str.codePointAt(i);
			switch (code) {
				case 8 		: sb.append("\\b"); break;
				case 9 		: sb.append("\\t"); break;
				case 10 	: sb.append("\\n"); break;
				//case 11 	: sb.append("\\v"); break;
				case 12		: sb.append("\\f"); break;
				case 13 	: sb.append("\\r"); break;
				case 34		: sb.append("\\\""); break;
				//case 47	: sb.append("\\/"); break;
				case 92 	: sb.append("\\\\"); break;
				case 8232	: sb.append("\\u2028"); break;
				case 8233	: sb.append("\\u2029"); break;
				default 	: {
					if( code<16 )
			 			sb.append("\\u000").append(Integer.toHexString(code));
					else if( code<32 || code>=127 && code<=159)
			 			sb.append("\\u00" ).append(Integer.toHexString(code));
					else
						sb.append( str.charAt(i) );
				}
			}
		}
		return sb.toString();		
	}
	
	public static String forJson(Object obj) {
		return forJson(obj, null);
	}

	private static String forJson(Object obj, String prefix) {
		if( obj==null )
			return "null";
		Class<?> clas = obj.getClass();
		if( JsonSupport.class.isAssignableFrom(clas) )
			return prefixJson(((JsonSupport)obj).toJson(), prefix).toString();
		else if( Literal.class.isAssignableFrom(clas) )
			return prefixJson(new Literal(obj.toString()), prefix).toString(); //new Literal is to prevent touching original literal
		else if( Boolean.class.isAssignableFrom(clas) )
			return obj.toString();
		else if( Character.class.isAssignableFrom(clas) || String.class.isAssignableFrom(clas) )
			return "\"" + escapeJson(obj.toString()) + "\"";
		else if( Number.class.isAssignableFrom(clas) )
			return obj.toString();
		else if( java.sql.Date.class.isAssignableFrom(clas) )
			return "\"" + DateUtil.formatISODate((java.sql.Date)obj) + "\"";
		else if( java.time.LocalDate.class.isAssignableFrom(clas) )
			return "\"" + DateUtil.formatISOLocalDate((java.time.LocalDate)obj) + "\"";
		else if( java.util.Date.class.isAssignableFrom(clas) )
			return "\"" + DateUtil.formatISODateTime((Date)obj) + "\"";
		else if( java.util.Calendar.class.isAssignableFrom(clas) )
			return "\"" + DateUtil.formatISODateTime(((Calendar)obj).getTime()) + "\"";
		else if( java.time.LocalDateTime.class.isAssignableFrom(clas) )
			return "\"" + DateUtil.formatISOLocalDateTime((java.time.LocalDateTime)obj) + "\"";
		else if( java.util.Collection.class.isAssignableFrom(clas) )
			return prefixJson(buildArray((Collection<?>)obj), prefix).toString();
		else if( java.util.Map.class.isAssignableFrom(clas) )
			return prefixJson(buildObject((Map<?, ?>)obj), prefix).toString();
		else if( clas.isArray() )
			return prefixJson(buildArray(obj), prefix).toString();
		else
			throw new RuntimeException("Unsupported type : " + clas.getCanonicalName());
	}

	public static Literal literal(String val) {
		return new Literal(val);
	}
	
	public interface JsonSupport {
		public Literal toJson();
	}

	public static class Literal {
		private String prefix = "";
		public Literal() {
			this.val = new StringBuilder();
		}
		public Literal(String val) {
			this.val = new StringBuilder(val);
		}
		StringBuilder val;
		@Override
		public String toString() {
			return val.toString();
		}
		public Literal beginArray() {
			char ch = val.length()>0 ? val.charAt(val.length()-1) : '\0';
			if( val.length()>0 && ch!=',' && ch!='[' && ch!='{' && ch!=':' )
				val.append(',');
			val.append('[');
			prefix += '\t';
			return this;
		}
		public Literal beginArray(String key) {
			char ch = val.length()>0 ? val.charAt(val.length()-1) : '\0';
			if( val.length()>0 && ch!=',' && ch!='[' && ch!='{' )
				val.append(',');
			if( val.length()>0 )
				val.append('\n').append(prefix);
			val.append( forJson(key) ).append(':').append('[');
			prefix += '\t';
			return this;
		}
		public Literal endArray() {
			prefix = prefix.substring(0, prefix.length()-1);
			val.append('\n').append(prefix).append(']');
			return this;
		}
		public Literal beginObject() {
			char ch = val.length()>0 ? val.charAt(val.length()-1) : '\0';
			if( val.length()>0 && ch!=',' && ch!='[' && ch!='{' && ch!=':' ) 
				val.append(',');
			val.append('{');
			prefix += '\t';
			return this;
		}
		public Literal beginObject(String key) {
			char ch = val.length()>0 ? val.charAt(val.length()-1) : '\0';
			if( val.length()>0 && ch!=',' && ch!='[' && ch!='{' )
				val.append(',');
			if( val.length()>0 )
				val.append('\n').append(prefix);
			val.append( forJson(key) ).append(':').append('{');
			prefix += '\t';
			return this;
		}
		public Literal endObject() {
			prefix = prefix.substring(0, prefix.length()-1);
			val.append('\n').append(prefix).append('}');
			return this;
		}
		public Literal append(Object obj) {
			if( val.charAt(val.length()-1)!='[' )
				val.append(',');
			val.append('\n').append(prefix).append( forJson(obj, prefix) );
			return this;
		}
		public Literal appendTextJson(String textJson) {
			if( val.charAt(val.length()-1)!='[' )
				val.append(',');
			val.append('\n').append(prefix).append( textJson );
			return this;
		}
		public Literal append(String key, Object obj) {
			if( val.charAt(val.length()-1)!='{' )
				val.append(',');
			val.append('\n').append(prefix).append( forJson(key) ).append(':').append( forJson(obj, prefix) );
			return this;
		}
		public Literal append(boolean ifTrue, String key, Object obj) {
			if( ifTrue ) {
				if( val.charAt(val.length()-1)!='{' )
					val.append(',');
				val.append('\n').append(prefix).append( forJson(key) ).append(':').append( forJson(obj, prefix) );
			}
			return this;
		}
		public <T> Literal append(String key, T[] arr, String... fieldNames) {
			return append(key, Arrays.asList(arr), fieldNames);
		}
		public <T> Literal append(String key, List<T> list, String... fieldNames) {
			if( list==null || list.isEmpty() )
				return beginArray(key).endArray();
			try {
				Class<?> clas = list.get(0).getClass();
				PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clas).getPropertyDescriptors();
				Map<String, PropertyDescriptor> propertyMap = new HashMap<>(propertyDescriptors.length);
				for(PropertyDescriptor propertyDescriptor : propertyDescriptors)
					propertyMap.put(propertyDescriptor.getName(), propertyDescriptor);
				
				Object[] methodOrFields = new Object[fieldNames.length];
				int i = 0;
				for(String fieldName : fieldNames) {
					if( propertyMap.containsKey(fieldName) ) {
						Method readMethod = propertyMap.get(fieldName).getReadMethod();
						methodOrFields[i] = readMethod==null ? clas.getField(fieldName) : readMethod;
					}
					else {
						try {
							methodOrFields[i] = clas.getDeclaredField(fieldName);
						}
						catch(NoSuchFieldException ex) {
							methodOrFields[i] = clas.getField(fieldName);
						}
					}
					i++;
				}
				beginArray(key);
				for(T el : list) {
					beginObject();
					i = 0;
					for(Object accessor : methodOrFields) {
						if( accessor instanceof Field )
							append(fieldNames[i], ((Field)accessor).get(el));
						else
							append(fieldNames[i], ((Method)accessor).invoke(el));
						i++;
					}
					endObject();
				}
				endArray();
			}
			catch(Exception ex) {
				throw new RuntimeException(ex);
			}
			return this;
		}
		public Literal appendTextJson(String key, String textJson) {
			if( val.charAt(val.length()-1)!='{' )
				val.append(',');
			val.append('\n').append(prefix).append( forJson(key) ).append(':').append( textJson );
			return this;
		}
		public void clear() {
			this.val.setLength(0);
		}
	}
	
	static class C {
		int id;
		String name;
		
		public C(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	
	public static void main(String[] args) {
		Literal lit = new Literal();
		lit.beginObject()
			.append("bir", 1)
			.append("arr", new int[] {1,2,3})
			.beginArray("arr2").append(1).append(2).append(3).appendTextJson("4,5").append(6).endArray()
			.appendTextJson("arr3", "[1,2,3]")
			.append("arr4", new C[] { new C(1, "Bir"), new C(2, "İki"), new C(3, "Üç") }, "id", "name")
			.endObject();
	
		System.out.println(lit.toString());
	}
}
