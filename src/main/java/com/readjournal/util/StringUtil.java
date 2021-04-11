package com.readjournal.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StringUtil {
	public final static Locale trLocale = new Locale("tr", "TR");
	public final static String NEW_LINE = System.getProperty("line.separator");

	public static String wrapSingle(String value) {
		if( value==null )
			return null;
		return "'" + value.replaceAll("\\'", "\\\\'") + "'";
	}

	public static String wrapDouble(String value) {
		if( value==null )
			return null;
		return "\"" + value.replaceAll("\\\"", "\\\\\"") + "\"";
	}

	public static boolean empty(String str) {
		return str==null || str.trim().length()==0;
	}

	public static boolean equals(String str1, String str2){
		return Utils.equals(str1, str2);
	}

	public static boolean isInArray(String str, String ... array) {
		for(String str2 : array)
			if( Utils.equals(str, str2) )
				return true;
		return false;
	}

	public static boolean equalsIgnoreCase(String str1, String str2) {
		return Utils.equals( str1!=null ? str1.toLowerCase(trLocale):null ,
					str2!=null ? str2.toLowerCase(trLocale):null);
	}

	public static String ifNull(String value) {
		return ifNull(value, "");
	}

	public static String ifNull(String value, String defaultValue) {
		if( value==null )
			return defaultValue;
		else
			return value;
	}

	public static String ifNull(String value1, String value2, String value3, String... values) {
		if( value1!=null )
			return value1;
		if( value2!=null )
			return value2;
		if( value3!=null )
			return value3;
		for(String value : values) {
			if( value!=null )
				return value;
		}
		return null;
	}

	public static String ifEmpty(String value) {
		return ifEmpty(value, null);
	}

	public static String ifEmpty(String value, String defaultValue) {
		if( empty(value) )
			return defaultValue;
		else
			return value;
	}

	public static String ifEmpty(String value1, String value2, String value3, String... values) {
		if( !empty(value1) )
			return value1;
		if( !empty(value2) )
			return value2;
		if( !empty(value3) )
			return value3;
		for(String value : values) {
			if( !empty(value) )
				return value;
		}
		return null;
	}

	public static String toCamelCase(String str) {
		if( empty(str) )
			return str;
		if( str.length()==1 )
			return str.toUpperCase( trLocale );
		return 	str.substring(0,1).toUpperCase( trLocale ) +
				str.substring(1).toLowerCase( trLocale );
	}

	public static String fullName(String fname, String lname) {
		return fullName(fname, lname, " ");
	}

	public static String fullName(String fname, String lname, String sep) {
		if( !empty(fname) && !empty(lname) )
			return fname + sep + lname;
		return StringUtil.ifEmpty(fname, StringUtil.ifEmpty(lname, ""));
	}

	public static String maxLength(String str, int maxLen) {
		if( str==null || str.length()<=maxLen )
			return str;
		return str.substring(0, maxLen);
	}

	public static String maxLength(String str, int maxLen, String tail) {
		if( str==null || str.length()<=maxLen )
			return str;
		return str.substring(0, maxLen-tail.length()) + tail;
	}

	public static int countOf(String str, String part) {
		if( empty(str) )
			return 0;
		int c = 0;
		int ind = -1;
		while( (ind = str.indexOf(part, ind+1))!=-1 )
			c++;
		return c;
	}

	// ("a.b.c.", '.') = "a"
	public static String beforeFirst(String str, char ch) {
		int ind = str.indexOf(ch);
		if( ind!=-1 )
			return str.substring(0, ind);
		return str;
	}

	// ("a.b.c.", '.') = "a.b"
	public static String beforeLast(String str, char ch) {
		int ind = str.lastIndexOf(ch);
		if( ind!=-1 )
			return str.substring(0, ind);
		return str;
	}

	/**
	 * this replaces white spaces with dashes and replaces turkish character with counterpart english chars
	 * @param str to be cleaned
	 * @return
	 */
	public static String cleanString(String str) {
		if( empty(str) )
			return str;
		StringBuilder sb = new StringBuilder(str);
		int i = 0;
		while( i<sb.length() ) {
			char ch = sb.charAt(i);
			if( 	'a'<=ch && ch<='z' ||
					'A'<=ch && ch<='Z' ||
					'0'<=ch && ch<='9' ||
					ch=='.' ||
					ch=='_' ||
					ch=='-' ) { }
			else if( ch=='Ş' )
				sb.setCharAt(i, 'S');
			else if( ch=='ş' )
				sb.setCharAt(i, 's');
			else if( ch=='İ' )
				sb.setCharAt(i, 'i');
			else if( ch=='ı' )
				sb.setCharAt(i, 'i');
			else if( ch=='Ç' )
				sb.setCharAt(i, 'C');
			else if( ch=='ç' )
				sb.setCharAt(i, 'c');
			else if( ch=='Ğ' )
				sb.setCharAt(i, 'G');
			else if( ch=='ğ' )
				sb.setCharAt(i, 'g');
			else if( ch=='Ö' )
				sb.setCharAt(i, 'O');
			else if( ch=='ö' )
				sb.setCharAt(i, 'o');
			else if( ch=='Ü' )
				sb.setCharAt(i, 'U');
			else if( ch=='ü' )
				sb.setCharAt(i, 'u');
			else
				sb.setCharAt(i, '_');
			i++;
		}
		return sb.toString();
	}

	/**
	 * This replaces white spaces with dashes and replaces turkish character with counterpart english chars
	 * @param str
	 * @param dotPermission
	 * @return
	 */
	public static String cleanString(String str, boolean dotPermission) {
		if( StringUtil.empty(str) )
			return str;
		StringBuilder sb = new StringBuilder(str);
		int i = 0;
		while( i<sb.length() ) {
			char ch = sb.charAt(i);
			if( 	'a'<=ch && ch<='z' ||
					'A'<=ch && ch<='Z' ||
					'0'<=ch && ch<='9' ||
					ch=='_' ||
					ch=='-' ) {
			}
			else if( ch=='.' && dotPermission) {
			}
			else if( ch=='Ş' )
				sb.setCharAt(i, 'S');
			else if( ch=='ş' )
				sb.setCharAt(i, 's');
			else if( ch=='İ' )
				sb.setCharAt(i, 'i');
			else if( ch=='ı' )
				sb.setCharAt(i, 'i');
			else if( ch=='Ç' )
				sb.setCharAt(i, 'C');
			else if( ch=='ç' )
				sb.setCharAt(i, 'c');
			else if( ch=='Ğ' )
				sb.setCharAt(i, 'G');
			else if( ch=='ğ' )
				sb.setCharAt(i, 'g');
			else if( ch=='Ö' )
				sb.setCharAt(i, 'O');
			else if( ch=='ö' )
				sb.setCharAt(i, 'o');
			else if( ch=='Ü' )
				sb.setCharAt(i, 'U');
			else if( ch=='ü' )
				sb.setCharAt(i, 'u');
			else
				sb.setCharAt(i, '_');
			i++;
		}

		String cleanStr = sb.toString();
		cleanStr = cleanStr
				.replaceAll("\\_+", "_")
				.replaceAll("\\-+", "-")
				.replaceAll("\\.+", ".");
		cleanStr = cleanStr.toLowerCase();
		char firstChar = cleanStr.charAt(0);
		char lastChar = cleanStr.charAt(cleanStr.length() - 1);
		if (lastChar == '.' || lastChar == '_') {
			cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
		}
		if (firstChar == '.' || firstChar == '_') {
			cleanStr = cleanStr.substring(1);
		}

		return cleanStr;
	}

	public static String stripHtml(String html, boolean keepNewLine) {
		HtmlStripper stripper = new HtmlStripper(keepNewLine);
		String text = stripper.parse(html);
		return text;
	}

	public static String stripHtml(String html) {
		return stripHtml(html, false);
	}

	public static boolean isValidInteger(String str) {
		try {
			Integer.parseInt(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidLong(String str) {
		try {
			Long.parseLong(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidFloat(String str) {
		try {
			Float.parseFloat(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidDouble(String str) {
		try {
			Double.parseDouble(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidBoolean(String str) {
		try {
			Boolean.parseBoolean(str);
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public static boolean isValidUrl(String url) {
		try {
			if(url.indexOf("://")>0)
				new URL(url);
		}
		catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	public static String merge(String ... strs) {
		StringBuilder sb = new StringBuilder();
		for(String str : strs)
			if( str!=null )
				sb.append(str);
		return sb.toString();
	}

	public static final Collator turkishCollator = Collator.getInstance( trLocale );
	private static Map<Character, Integer> charToIndex;
	static {
		charToIndex = new HashMap<>(200);
		int k = 1_000_000;
		charToIndex.put('0', k++);
		charToIndex.put('1', k++);
		charToIndex.put('2', k++);
		charToIndex.put('3', k++);
		charToIndex.put('4', k++);
		charToIndex.put('5', k++);
		charToIndex.put('6', k++);
		charToIndex.put('7', k++);
		charToIndex.put('8', k++);
		charToIndex.put('9', k++);
		charToIndex.put('A', k);
		charToIndex.put('a', k++);
		charToIndex.put('Á', k);
		charToIndex.put('á', k++);
		charToIndex.put('À', k);
		charToIndex.put('à', k++);
		charToIndex.put('Â', k);
		charToIndex.put('â', k++);
		charToIndex.put('Å', k);
		charToIndex.put('å', k++);
		charToIndex.put('Ã', k);
		charToIndex.put('ã', k++);
		charToIndex.put('Ä', k);
		charToIndex.put('ä', k++);
		charToIndex.put('Æ', k);
		charToIndex.put('æ', k++);
		charToIndex.put('B', k);
		charToIndex.put('b', k++);
		charToIndex.put('C', k);
		charToIndex.put('c', k++);
		charToIndex.put('Ç', k);
		charToIndex.put('ç', k++);
		charToIndex.put('D', k);
		charToIndex.put('d', k++);
		charToIndex.put('E', k);
		charToIndex.put('e', k++);
		charToIndex.put('É', k);
		charToIndex.put('é', k++);
		charToIndex.put('È', k);
		charToIndex.put('è', k++);
		charToIndex.put('Ê', k);
		charToIndex.put('ê', k++);
		charToIndex.put('Ë', k);
		charToIndex.put('ë', k++);
		charToIndex.put('F', k);
		charToIndex.put('f', k++);
		charToIndex.put('G', k);
		charToIndex.put('g', k++);
		charToIndex.put('Ğ', k);
		charToIndex.put('ğ', k++);
		charToIndex.put('H', k);
		charToIndex.put('h', k++);
		charToIndex.put('I', k);
		charToIndex.put('ı', k++);
		charToIndex.put('İ', k);
		charToIndex.put('i', k++);
		charToIndex.put('Í', k);
		charToIndex.put('í', k++);
		charToIndex.put('Ì', k);
		charToIndex.put('ì', k++);
		charToIndex.put('Î', k);
		charToIndex.put('î', k++);
		charToIndex.put('Ï', k);
		charToIndex.put('ï', k++);
		charToIndex.put('J', k);
		charToIndex.put('j', k++);
		charToIndex.put('K', k);
		charToIndex.put('k', k++);
		charToIndex.put('L', k);
		charToIndex.put('l', k++);
		charToIndex.put('M', k);
		charToIndex.put('m', k++);
		charToIndex.put('N', k);
		charToIndex.put('n', k++);
		charToIndex.put('Ñ', k);
		charToIndex.put('ñ', k++);
		charToIndex.put('O', k);
		charToIndex.put('o', k++);
		charToIndex.put('Ó', k);
		charToIndex.put('ó', k++);
		charToIndex.put('Ò', k);
		charToIndex.put('ò', k++);
		charToIndex.put('Ô', k);
		charToIndex.put('ô', k++);
		charToIndex.put('Õ', k);
		charToIndex.put('õ', k++);
		charToIndex.put('Ö', k);
		charToIndex.put('ö', k++);
		charToIndex.put('P', k);
		charToIndex.put('p', k++);
		charToIndex.put('Q', k);
		charToIndex.put('q', k++);
		charToIndex.put('R', k);
		charToIndex.put('r', k++);
		charToIndex.put('S', k);
		charToIndex.put('s', k++);
		charToIndex.put('ß', k++);
		charToIndex.put('Ş', k);
		charToIndex.put('ş', k++);
		charToIndex.put('T', k);
		charToIndex.put('t', k++);
		charToIndex.put('U', k);
		charToIndex.put('u', k++);
		charToIndex.put('Ú', k);
		charToIndex.put('ú', k++);
		charToIndex.put('Ù', k);
		charToIndex.put('ù', k++);
		charToIndex.put('Û', k);
		charToIndex.put('û', k++);
		charToIndex.put('Ü', k);
		charToIndex.put('ü', k++);
		charToIndex.put('V', k);
		charToIndex.put('v', k++);
		charToIndex.put('W', k);
		charToIndex.put('w', k++);
		charToIndex.put('X', k);
		charToIndex.put('x', k++);
		charToIndex.put('Y', k);
		charToIndex.put('y', k++);
		charToIndex.put('Ÿ', k);
		charToIndex.put('ÿ', k++);
		charToIndex.put('Z', k);
		charToIndex.put('z', k++);
	}

	public static int compareTurkish(String word1, String word2) {
		if( word1==null )
			return word2==null ? 0 : -1;
		if( word2==null )
			return 1;

		word1 = word1.toUpperCase(trLocale);
		word2 = word2.toUpperCase(trLocale);

		int len1 = word1.length(),
			len2 = word2.length(),
			len = Math.min(len1, len2);
		Integer ind1, ind2;
		char ch1, ch2;
		for(int i=0; i<len; i++) {
			ch1 = word1.charAt(i);
			ch2 = word2.charAt(i);
			ind1 = charToIndex.get(ch1);
			if( ind1==null )
				ind1 = (int)ch1;
			ind2 = charToIndex.get(ch2);
			if( ind2==null )
				ind2 = (int)ch2;
			if( ind1>ind2 )
				return 1;
			if( ind1<ind2 )
				return -1;
		}
		return len1<len2 ? -1 : len1>len2 ? 1 : 0;
	}

	public static final int compareTurkish2(String word1, String word2) {
		return  turkishCollator.compare( word1==null ? "" : word1.toUpperCase(trLocale),
										word2==null ? "" : word2.toUpperCase(trLocale) );
	}

	public static final Comparator<String> turkishAscComparator = new Comparator<String>() {
		@Override
		public int compare(String str1, String str2) {
			return compareTurkish(str1, str2);
		}
	};

	public static final Comparator<String> turkishDescComparator = new Comparator<String>() {
		@Override
		public int compare(String str1, String str2) {
			return compareTurkish(str2, str1);
		}
	};

	public static String replaceTurkishChars(String str) {
		StringBuilder sb = new StringBuilder(str);
		int len = sb.length();
		for(int i=0; i<len; i++) {
			switch(sb.charAt(i)) {
				case 'Ş' : sb.setCharAt(i, 'S'); break;
				case 'ş' : sb.setCharAt(i, 's'); break;
				case 'Ü' : sb.setCharAt(i, 'U'); break;
				case 'ü' : sb.setCharAt(i, 'u'); break;
				case 'Ö' : sb.setCharAt(i, 'O'); break;
				case 'ö' : sb.setCharAt(i, 'o'); break;
				case 'İ' : sb.setCharAt(i, 'I'); break;
				case 'ı' : sb.setCharAt(i, 'i'); break;
				case 'Ç' : sb.setCharAt(i, 'C'); break;
				case 'ç' : sb.setCharAt(i, 'c'); break;
				case 'Ğ' : sb.setCharAt(i, 'G'); break;
				case 'ğ' : sb.setCharAt(i, 'g'); break;
			}
		}
		return sb.toString();
	}
}
