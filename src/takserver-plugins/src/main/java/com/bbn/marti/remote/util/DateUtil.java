

package com.bbn.marti.remote.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateUtil {
	protected static final ThreadLocal<DateTimeFormatter> cotDateParser = new ThreadLocal<>();
	protected static final ThreadLocal<DateTimeFormatter> cotDateParserMillis = new ThreadLocal<>();
	protected static final ThreadLocal<DateTimeFormatter> iso = new ThreadLocal<>();
	
	private static DateTimeFormatter cotDateParser() {
		if (cotDateParser.get() == null) {
			cotDateParser.set(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC());
		}
		return cotDateParser.get();
	}
	
	private static DateTimeFormatter cotDateParserMillis() {
		if (cotDateParserMillis.get() == null) {
			cotDateParserMillis.set(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC());
		}
		return cotDateParserMillis.get();
	}
	
	private static DateTimeFormatter iso() {
		if (iso.get() == null) {
			iso.set(ISODateTimeFormat.dateTime());
		}
		return iso.get();
	}

	public static DateTime dateTimeFromCotTimeStr(String timeStr) {
		DateTime r;
		if (timeStr.contains(".")) {
			r = iso().parseDateTime(timeStr);
		} else {
			r = cotDateParser().parseDateTime(timeStr);
		}
		return r;
	}

	public static final long millisFromCotTimeStr(String timeStr) {
		return dateTimeFromCotTimeStr(timeStr).getMillis();
	}
	

	public static final String toCotTime(long millisSinceEpochUtc) {
		return cotDateParser().print(millisSinceEpochUtc);
	}

	public static final String toCotTimeMillis(long millisSinceEpochUtc) {
		return cotDateParserMillis().print(millisSinceEpochUtc);
	}
}
