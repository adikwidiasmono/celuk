package com.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adikwidiasmono on 11/13/16.
 */

public class AppDateUtils {
    public static final String APP_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static Date parseDateToString(String inputDate, String datePattern) {
        DateFormat dateFormat = new SimpleDateFormat(datePattern);
        try {
            return dateFormat.parse(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String parseStringToDate(Date inputDate, String datePattern) {
        DateFormat dateFormat = new SimpleDateFormat(datePattern);
        return dateFormat.format(inputDate);
    }

    public static String getCurrentDate(String datePattern) {
        DateFormat dateFormat = new SimpleDateFormat(datePattern);
        return dateFormat.format(getCurrentDate());
    }

    public static Date getCurrentDate() {
        return new Date();
    }
}
