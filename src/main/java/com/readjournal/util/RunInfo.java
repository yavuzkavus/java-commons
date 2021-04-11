package com.readjournal.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class RunInfo {
	long start;

	public RunInfo() {
		this.start = System.currentTimeMillis();
	}

	public void printInfo() {
		printInfo(null);
	}

	public void printInfo(String formatString, Object... args) {
		if( args!=null && args.length>0 ) {
			for(int i=0, len=args.length; i<len; i++) {
				if( args[i] instanceof java.sql.Date )
					args[i] = DateUtil.formatDate((Date)args[i]);
				else if( args[i] instanceof Date )
					args[i] = DateUtil.formatDateTime((Date)args[i]);
				else if( args[i] instanceof LocalDate )
					args[i] = DateUtil.formatLocalDate((LocalDate)args[i]);
				else if( args[i] instanceof LocalDateTime )
					args[i] = DateUtil.formatLocalDateTime((LocalDateTime)args[i]);
			}
		}
		StackTraceElement stackEl = null;
		StackTraceElement stackEls[] = Thread.currentThread().getStackTrace();
		for(int i=1, len=stackEls.length; i<len; i++) {
			if( stackEls[i].toString().indexOf(RunInfo.class.getName())==-1 ) {
				stackEl = stackEls[i];
				break;
			}
		}
		System.out.printf(
				"On: " + DateUtil.formatDateTime(new Date()) +
				" In: " +  DateUtil.formatDuration(System.currentTimeMillis()-start) +
				" Code: " + stackEl + "%n" +
				(formatString!=null ? formatString + "%n" : ""),
				args);
	}
}
