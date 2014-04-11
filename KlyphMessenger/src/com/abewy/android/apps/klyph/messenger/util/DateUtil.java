package com.abewy.android.apps.klyph.messenger.util;

import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang3.time.FastDateFormat;
import android.util.Log;

public class DateUtil
{
	private static FastDateFormat getDateFormat()
	{
		FastDateFormat dateFormat = FastDateFormat.getDateInstance(FastDateFormat.FULL);
		String pattern = dateFormat.getPattern();

		pattern = pattern.replace("y", "");
		pattern = pattern.replace("E", "");
		pattern = pattern.replace(",", "");
		pattern = pattern.replace("  ", " ");
		pattern = pattern.trim();

		return FastDateFormat.getInstance(pattern);
	}

	private static String getFormattedDate(Date date)
	{
		return getDateFormat().format(date);
	}

	private static String getFormattedFullDate(Date date)
	{
		FastDateFormat dateFormat = FastDateFormat.getDateInstance(FastDateFormat.FULL);
		String pattern = dateFormat.getPattern();

		pattern = pattern.replace("E", "");
		pattern = pattern.replace(",", "");
		pattern = pattern.replace("  ", " ");
		pattern = pattern.trim();
		
		return FastDateFormat.getInstance(pattern).format(date);
	}

	private static String getFormattedTime(Date date)
	{
		return FastDateFormat.getTimeInstance(FastDateFormat.SHORT).format(date);
	}

	public static String getFormattedDateTime(String unixDate)
	{
		Date date;

		try
		{
			long when = Long.parseLong(unixDate);
			date = new Date(when * 1000);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return "";
		}

		return new StringBuilder(getFormattedDate(date)).append(" ").append(getFormattedTime(date)).toString();
	}

	public static String getFormattedDateTimeWithYear(String unixDate)
	{
		Date date;

		try
		{
			long when = Long.parseLong(unixDate);
			date = new Date(when * 1000);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return "";
		}

		return new StringBuilder(getFormattedFullDate(date)).append(" ").append(getFormattedTime(date)).toString();
	}

	public static String getShortDate(String unixDate)
	{
		Date date = new Date(Long.parseLong(unixDate)*1000);
		Date now = new Date();
		
		
		String pattern = "";
		
		// If date < 7 days
		if (now.getTime() - date.getTime() < 7 * 24 * 60 * 60 * 1000)
		{
			pattern = "E";
		}
		else
		{
			FastDateFormat dateFormat = FastDateFormat.getDateInstance(FastDateFormat.MEDIUM);
			pattern = dateFormat.getPattern();
			pattern = pattern.replace("y", "");
			pattern = pattern.replace("年", "");//y in chinese
			pattern = pattern.replace(",", "");
			pattern = pattern.replace("  ", " ");
			pattern = pattern.trim();
			
			if (pattern.indexOf("/") == 0 || pattern.indexOf("-") == 0 || pattern.indexOf(".") == 0)
			{
				pattern = pattern.substring(1);
			}
		}
		
		FastDateFormat dateFormat = FastDateFormat.getInstance(pattern);
		
		return dateFormat.format(date);
	}
	
	public static String getShortDateTime(String unixDate)
	{
		Date date = new Date(Long.parseLong(unixDate)*1000);
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		c2.setTime(date);
		
		
		FastDateFormat dateFormat = FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);
		String pattern = dateFormat.getPattern();
		
		// If not same year
		if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))
		{
			pattern = pattern.replace("y", "");
			
			if (pattern.indexOf("/") == 0 || pattern.indexOf("-") == 0 || pattern.indexOf(".") == 0  || pattern.indexOf("年") == 0)
			{
				pattern = pattern.substring(1);
			}
			
/*			pattern = pattern.replace("EEEE", "EEE");
			pattern = pattern.replace("MMMM", "");
			pattern = pattern.replace("d", "");
		}
		else
		{
			pattern = pattern.replace("MMMM", "MMM");
			pattern = pattern.replace("EEEE", "");*/
		}
		
		pattern = pattern.replace("  ", " ");
		pattern = pattern.trim();
		
		dateFormat = FastDateFormat.getInstance(pattern);
		
		return dateFormat.format(date);
	}
}
