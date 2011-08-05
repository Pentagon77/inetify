package net.luniks.android.inetify;

import java.util.Date;

import android.content.Context;
import android.text.format.DateFormat;

/**
 * Some static helper methods.
 * 
 * @author torsten.roemer@luniks.net
 */
public class Utils {
	
	private Utils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Returns the given timestamp formatted as date and time for the default locale.
	 * @param context context
	 * @param timestamp timestamp to format
	 * @return String timestamp formatted as date and time
	 */
	public static String getDateTimeString(final Context context, final long timestamp) {
		Date date = new Date(timestamp);
		String dateString = DateFormat.getLongDateFormat(context).format(date);
		String timeString = DateFormat.getTimeFormat(context).format(date);
		return String.format("%s, %s", dateString, timeString);
	}
	
}