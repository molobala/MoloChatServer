package com.molo;

import java.util.Calendar;

public class DateUtils {
	public static String getCurrentInString(){
		Calendar c=Calendar.getInstance();
		String d="";
		d+=c.get(Calendar.YEAR)+"-";
		d+=(c.get(Calendar.MONTH)+1)+"-";
		d+=c.get(Calendar.DATE);
		return d;
	}
}
