/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.base.util;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class Utils {

    /**
     * return a 'yyyy-MM-dd' represantation of the date passed. For usage in
     * cassandra key buckets
     *
     * @param date
     * @return
     */
    public static String extractYearMonthDayLiteral(Date date) {
        if (date == null) {
            return "UNDEFINED";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // MONTH is zero based.
        String sMonth = String.valueOf(month);
        if (sMonth.length() == 1) {
            sMonth = "0".concat(sMonth);
        }
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String sDay = String.valueOf(day);
        if (sDay.length() == 1) {
            sDay = "0".concat(sDay);
        }
        String year_month_day_bucket = String.valueOf(year).concat("-").concat(sMonth).concat("-").concat(sDay);
        return year_month_day_bucket;
    }

    /**
     * return a 'yyyy-MM-dd' represantation of the timestamp passed. For usage in
     * cassandra key buckets
     *
     * @param timestamp
     * @return
     */
    public static String extractYearMonthDayLiteral(long timestamp) {
        if (timestamp == 0l) {
            return "UNDEFINED";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // MONTH is zero based.
        String sMonth = String.valueOf(month);
        if (sMonth.length() == 1) {
            sMonth = "0".concat(sMonth);
        }
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String sDay = String.valueOf(day);
        if (sDay.length() == 1) {
            sDay = "0".concat(sDay);
        }
        String year_month_day_bucket = String.valueOf(year).concat("-").concat(sMonth).concat("-").concat(sDay);
        return year_month_day_bucket;
    }

}
