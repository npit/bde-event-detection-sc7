/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.crawlers.util;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class Utils {

    /**
     * return a 'yyyy_MM_dd' represantation of the date passed
     *
     * @param date
     * @return
     */
    public static String extractYearMonthDayLiteral(Date date) {
        if (date == null) {
            return "";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String year_month_day_bucket = String.valueOf(year).concat("_").concat(String.valueOf(month)).concat("_").concat(String.valueOf(day));
        return year_month_day_bucket;
    }

}
