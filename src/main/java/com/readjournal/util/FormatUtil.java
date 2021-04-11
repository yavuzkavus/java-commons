package com.readjournal.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

public class FormatUtil {
	private static final NumberFormat MONEY_FORMAT		= new DecimalFormat("#,##0.##", new DecimalFormatSymbols(StringUtil.trLocale));
	private static final NumberFormat MONEY_FORMAT_USD	= NumberFormat.getCurrencyInstance( Locale.US );
	private static final NumberFormat MONEY_FORMAT_EURO	= NumberFormat.getCurrencyInstance( Locale.GERMANY );
	private static final NumberFormat MONEY_FORMAT_TL	= NumberFormat.getCurrencyInstance( StringUtil.trLocale );

	/*
	public static String formatBytes(long bytes) {
		String sign = "";
		if( bytes<0 ) {
			sign = "-";
			bytes = -bytes;
		}
		if( bytes<1024 )
			return sign + bytes + "B";
		int kbytes = (int)(bytes/1024);
		if( kbytes<1024 )
			return sign + kbytes +"KB";
		int mbytes = (kbytes/1024);
		if( mbytes<1024 )
			return sign + mbytes +"MB";
		int gbytes = (mbytes/1024);
		return sign + gbytes + "." + ((kbytes%1024)) + "GB";
	}
	*/

	public static String formatMoney(double money) {
		return MONEY_FORMAT.format(money);
	}

	public static synchronized String formatMoneyTL(double money) {
		return MONEY_FORMAT_TL.format(money);
	}

	public static synchronized String formatMoneyUSD(double money) {
		return MONEY_FORMAT_USD.format(money);
	}

	public static synchronized String formatMoneyEuro(double money) {
		return MONEY_FORMAT_EURO.format(money);
	}

	public static String formatBytes(long bytes) {
		//http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
		if( bytes < 1024 )
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String pre = "KMGTPE".charAt(exp-1) + "i";
		return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
	}

	public static String formatLong(long number) {
		String numberStr = String.valueOf(number);
		if( number<1000 )
			return numberStr;
		StringBuilder sb = new StringBuilder();
		int i = 0,
			len = numberStr.length();
		if( len%3!=0 ) {
			sb.append(numberStr.substring(i, len%3)).append('.');
			i += len%3;
		}
		while(i<len) {
			sb.append(numberStr.substring(i, i+3)).append('.');
			i += 3;
		}
		sb.setLength(sb.length()-1);
		return sb.toString();
	}

	public static String formatLong(long l, int len) {
		NumberFormat nf = NumberFormat.getNumberInstance(StringUtil.trLocale);
		char[] temp = new char[len>3 ? len+1 : len];
		Arrays.fill(temp, '0');
		if( len>3 )
			temp[ len-3 ] = ',';
		((DecimalFormat)nf).applyPattern( new String(temp) );
		return nf.format(l);
	}
}
