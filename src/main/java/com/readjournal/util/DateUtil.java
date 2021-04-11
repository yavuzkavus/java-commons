package com.readjournal.util;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.time.FastDateFormat;

public class DateUtil {
	public static final long HOUR_MILLIS	= 60L * 60 * 1000;
	public static final long DAY_MILLIS		= 24L * HOUR_MILLIS;
	public static final long WEEK_MILLIS	= 7L * DAY_MILLIS;
	public static final long MONTH_MILLIS	= 30L * DAY_MILLIS;
	public static final long YEAR_MILLIS	= 365L * DAY_MILLIS;

	public static final FastDateFormat DATE_FDF						= FastDateFormat.getInstance("dd.MM.yyyy");
	public static final FastDateFormat DATETIME_FDF					= FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");
	public static final FastDateFormat SIMPLE_FDF					= FastDateFormat.getInstance("ddMMyyyy");

	public static final DateTimeFormatter LOCALDATE_DTF				= DateTimeFormatter.ofPattern("dd.MM.yyyy");
	public static final DateTimeFormatter LOCALDATETIME_DTF			= DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
	public static final DateTimeFormatter SIMPLE_DTF				= DateTimeFormatter.ofPattern("ddMMyyyy");

	public static final FastDateFormat ISO_DATE_FDF					= FastDateFormat.getInstance("yyyy-MM-dd");
	public static final FastDateFormat ISO_DATETIME_FDF				= FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS");
	public static final FastDateFormat ISO_SIMPLE_DATE_FDF			= FastDateFormat.getInstance("yyyyMMdd");

	public static final DateTimeFormatter ISO_LOCALDATE_DTF			= DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter ISO_LOCALDATETIME_DTF		= DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
	public static final DateTimeFormatter ISO_SIMPLE_LOCALDATE_DTF	= DateTimeFormatter.ofPattern("yyyyMMdd");


	public static Date createDate(int year, int month, int day, int hour, int minute, int second) {
		return new GregorianCalendar(year, month, day, hour, minute, second).getTime();
	}

	public static Date createDate(int year, int month, int day, int hour, int minute) {
		return createDate( year, month, day, hour, minute, 0 );
	}

	public static Date createDate(int year, int month, int day, int hour) {
		return createDate( year, month, day, hour, 0, 0 );
	}

	public static Date createDate(int year, int month, int day) {
		return createDate( year, month, day, 0, 0, 0 );
	}

	public static Date parseDate(String dateStr) {
		try {
			return DATE_FDF.parse(dateStr);
		}
		catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static Date parseSimpleDate(String dateStr) {
		try {
			return SIMPLE_FDF.parse(dateStr);
		}
		catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static LocalDate parseLocalDate(String dateStr) {
		try {
			return LocalDate.parse(dateStr, LOCALDATE_DTF);
		}
		catch(DateTimeParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static Date parseDateTime(String dateStr) {
		try {
			return DATETIME_FDF.parse(dateStr);
		}
		catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static LocalDateTime parseLocalDateTime(String dateStr) {
		try {
			return LocalDateTime.parse(dateStr, LOCALDATETIME_DTF);
		}
		catch(DateTimeParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static String formatDate(Date date) {
		return DATE_FDF.format(date);
	}

	public static String formatDate(Calendar cal) {
		return DATE_FDF.format(cal);
	}

	public static String formatSimpleDate(Date date) {
		return SIMPLE_FDF.format(date);
	}

	public static String formatSimpleDate(Calendar cal) {
		return SIMPLE_FDF.format(cal);
	}

	public static String formatLocalDate(LocalDate date) {
		return LOCALDATE_DTF.format(date);
	}

	public static String formatDateTime(Date date) {
		return DATETIME_FDF.format(date);
	}

	public static String formatDateTime(Calendar cal) {
		return DATETIME_FDF.format(cal);
	}

	public static String formatLocalDateTime(LocalDateTime date) {
		return LOCALDATETIME_DTF.format(date);
	}

	public static Date parseISODate(String dateStr) {
		try {
			return ISO_DATE_FDF.parse(dateStr);
		}
		catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static Date parseISOSimpleDate(String dateStr) {
		try {
			return ISO_SIMPLE_DATE_FDF.parse(dateStr);
		}
		catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static LocalDate parseISOLocalDate(String dateStr) {
		try {
			return LocalDate.parse(dateStr, ISO_LOCALDATE_DTF);
		}
		catch (DateTimeParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static Date parseISODateTime(String dateStr) {
		try {
			return ISO_DATETIME_FDF.parse(dateStr);
		}
		catch (ParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static LocalDateTime parseISOLocalDateTime(String dateStr) {
		try {
			return LocalDateTime.parse(dateStr, ISO_LOCALDATETIME_DTF);
		}
		catch (DateTimeParseException e) {
			throw Utils.runtime(e);
		}
	}

	public static String formatISODate(Date date) {
		return ISO_DATE_FDF.format(date);
	}

	public static String formatISODate(Calendar cal) {
		return ISO_DATE_FDF.format(cal);
	}

	public static String formatISOSimpleDate(Date date) {
		return ISO_SIMPLE_DATE_FDF.format(date);
	}

	public static String formatISOSimpleDate(Calendar cal) {
		return ISO_SIMPLE_DATE_FDF.format(cal);
	}

	public static String formatISOLocalDate(LocalDate date) {
		return ISO_LOCALDATE_DTF.format(date);
	}

	public static String formatISOSimpleLocalDate(LocalDate date) {
		return ISO_SIMPLE_LOCALDATE_DTF.format(date);
	}

	public static String formatISODateTime(Date date) {
		return ISO_DATETIME_FDF.format(date);
	}

	public static String formatISODateTime(Calendar cal) {
		return ISO_DATETIME_FDF.format(cal);
	}

	public static String formatISOLocalDateTime(LocalDateTime date) {
		return ISO_LOCALDATETIME_DTF.format(date);
	}

	public static String convertDatePattern(String date, String currentPattern, String nextPattern) {
		return convertDatePattern(date, FastDateFormat.getInstance(currentPattern), FastDateFormat.getInstance(nextPattern));
	}

	public static String convertDatePattern(String dateStr, DateTimeFormatter currentDtf, DateTimeFormatter nextDtf) {
		TemporalAccessor date = currentDtf.parse(dateStr);
		return nextDtf.format(date);
	}

	public static String convertDatePattern(String dateStr, FastDateFormat currentFdf, FastDateFormat nextFdf) {
		try {
			Date date = currentFdf.parse(dateStr);
			return nextFdf.format(date);
		}
		catch(ParseException pe) {
			throw Utils.runtime(pe);
		}
	}

	public static Calendar toCalendar(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	public static void startOfDay(Calendar cal){
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	public static Date startOfDay(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		startOfDay(cal);
		return cal.getTime();
	}

	public static void startOfHour(Calendar cal){
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	public static Date startOfHour(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		startOfHour(cal);
		return cal.getTime();
	}

	public static void startOfMinute(Calendar cal){
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	public static Date startOfMinute(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		startOfMinute(cal);
		return cal.getTime();
	}

	public static void startOfNextDay(Calendar cal){
		cal.add(Calendar.DAY_OF_YEAR, 1);
		startOfDay(cal);
	}

	public static Date startOfNextDay(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		startOfNextDay(cal);
		return cal.getTime();
	}

	public static void endOfDay(Calendar cal){
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 997); // not 999, because sql server rounds it to next day
	}

	public static Date endOfDay(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		endOfDay(cal);
		return cal.getTime();
	}

	@SafeVarargs
	public static <T extends Date> T min(T... dates) {
		T minDate = dates[0];
		for(int i=1; i<dates.length; i++) {
			if( dates[i].before(minDate) )
				minDate = dates[i];
		}
		return minDate;
	}

	@SafeVarargs
	public static <T extends Date> T max(T... dates) {
		T maxDate = dates[0];
		for(int i=1; i<dates.length; i++) {
			if( dates[i].after(maxDate) )
				maxDate = dates[i];
		}
		return maxDate;
	}

	public static int compareDate(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		return compareDate(cal1, cal2);
	}

	public static int compareDate(Calendar cal1, Calendar cal2) {
		if( cal1.get(Calendar.YEAR)!=cal2.get(Calendar.YEAR) )
			return cal1.get(Calendar.YEAR)>cal2.get(Calendar.YEAR) ? 1 : -1;
		if( cal1.get(Calendar.MONTH)!=cal2.get(Calendar.MONTH) )
			return cal1.get(Calendar.MONTH)>cal2.get(Calendar.MONTH) ? 1 : -1;
		if( cal1.get(Calendar.DATE)!=cal2.get(Calendar.DATE) )
			return cal1.get(Calendar.DATE)>cal2.get(Calendar.DATE) ? 1 : -1;
		return 0;
	}

	/**
	 * check whether date1 is after date2
	 * compare only year, month, day fields of related date object
	 * @param date1
	 * @param date2
	 * @return true for date1>date2, false otherwise
	 */
	public static boolean isDateAfter(Date date1, Date date2) {
		return compareDate(date1, date2)==1;
	}

	public static boolean isDateAfter(Calendar cal1, Calendar cal2) {
		return compareDate(cal1, cal2)==1;
	}

	/**
	 * check whether date1 is equal to date2
	 * compare only year, month, day fields of related date object
	 * @param date1
	 * @param date2
	 * @return true for date1==date2, false otherwise
	 */
	public static boolean isDateEqual(Date date1, Date date2) {
		return  compareDate(date1, date2)==0;
	}

	public static boolean isDateEqual(Calendar cal1, Calendar cal2) {
		return  compareDate(cal1, cal2)==0;
	}

	/**
	 * check whether date1 is before date2
	 * compare only year, month, day fields of related date object
	 * @param date1
	 * @param date2
	 * @return true for date1<date2, false otherwise
	 */
	public static boolean isDateBefore(Date date1, Date date2) {
		return compareDate(date1, date2)==-1;
	}

	public static boolean isDateBefore(Calendar cal1, Calendar cal2) {
		return compareDate(cal1, cal2)==-1;
	}

	/**
	 * check whether date is between date1 and date2
	 * compare only year, month, day fields of related date object
	 * @param date
	 * @param date1
	 * @param date2
	 * @return true for date>=date1 and date<=date2, false otherwise
	 */
	public static boolean isDateBetween(Date date, Date date1, Date date2) {
		return compareDate(date, date1)>=0 && compareDate(date, date2)<=0;
	}

	public static boolean isDateBetween(Calendar cal, Calendar cal1, Calendar cal2) {
		return compareDate(cal, cal1)>=0 && compareDate(cal, cal2)<=0;
	}

	/**
	 * check whether date is between date1 and date2
	 * compare millis of date1 and date2
	 * @param date
	 * @param date1
	 * @param date2
	 * @return true for date>=date1 and date<=date2, false otherwise
	 */
	public static boolean isDateTimeBetween(Date date, Date date1, Date date2) {
		long time = date.getTime();
		return time>=date1.getTime() && date2.getTime()>=time;
	}

	public static boolean isDateTimeBetween(Calendar cal, Calendar cal1, Calendar cal2) {
		long time = cal.getTimeInMillis();
		return time>=cal1.getTimeInMillis() && cal2.getTimeInMillis()>=time;
	}

	public static Date getDate(Date date, int days){
		return getDate(date, Calendar.DAY_OF_MONTH, days);
	}

	public static Date getDate(Date date, int type, int count){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(type, count);
		return new Date( cal.getTimeInMillis() );
	}

	public static int getYear(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.YEAR);
	}

	public static int getDaysBetween(Date past, Date next) {
		Calendar d1 = Calendar.getInstance(),
				 d2 = Calendar.getInstance();
		d1.setTime(past);
		d2.setTime(next);
		return getDaysBetween(d1, d2);
	}

	public static int getDaysBetween(java.util.Calendar d1, java.util.Calendar d2) {
		if (d1.after(d2)) {  // swap dates so that d1 is start and d2 is end
			java.util.Calendar swap = d1;
			d1 = d2;
			d2 = swap;
		}
		int days = d2.get(java.util.Calendar.DAY_OF_YEAR) -
				   d1.get(java.util.Calendar.DAY_OF_YEAR);
		int y2 = d2.get(java.util.Calendar.YEAR);
		if( d1.get(java.util.Calendar.YEAR)!=y2 ) {
			d1 = (java.util.Calendar) d1.clone();
			do {
				days += d1.getActualMaximum(java.util.Calendar.DAY_OF_YEAR);
				d1.add(java.util.Calendar.YEAR, 1);
			} while( d1.get(java.util.Calendar.YEAR)!=y2 );
		}
		return days;
	}

	public static int hoursBetween(Date past, Date next) {
		return (int)( (next.getTime() - past.getTime())/HOUR_MILLIS );
	}

	public static String formatDuration(long millis){
		int secs = (int)(millis/1000);
		int mins = secs/60;
		secs = secs%60;
		int hours = mins/60;
		mins = mins%60;
		return String.format("%02d:%02d:%02d", hours, mins, secs);
	}

	public static String formatDurationWithMillis(long millis){
		int secs = (int)(millis/1000);
		millis = millis % 1000;
		int mins = secs/60;
		secs = secs%60;
		int hours = mins/60;
		mins = mins%60;
		return String.format("%02d:%02d:%02d.%03d", hours, mins, secs, millis);
	}
}
