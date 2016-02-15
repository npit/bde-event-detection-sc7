/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.base.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
        return extractLiteral(cal);
        
    }

    /**
     * return a 'yyyy-MM-dd' representation of the timestamp passed. For usage
     * in cassandra key buckets
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
        return extractLiteral(cal);
    }
    
    private static String extractLiteral(Calendar cal) {
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
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * return a list containing the 'yyyy-MM-dd' representation of the timestamp
     * passed until the current date. If timestamp passed is 0, returns all days
     * from 30 days ago until now.
     *
     * @param timestamp
     * @return
     */
    public static List<String> extractYearMonthDayLiteralRangeFrom(long timestamp) {
        Calendar cal = Calendar.getInstance();
        long cur = cal.getTimeInMillis();
        String cur_literal = extractYearMonthDayLiteral(cur);
        List<String> res = new ArrayList();
        // in order to avoid 'Allow Filtering' in Cassandra, 
        // we have to calculate the discrete day literal for each day from the 
        // given timestamp. 
        if (timestamp >= cur) {
            res.add(cur_literal);
        } else {
            if (timestamp == 0l) {
                // set timestamp 1 month ago
                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
                timestamp = cal.getTimeInMillis();
            }
            String date_literal = extractYearMonthDayLiteral(timestamp);
            res.add(date_literal);
            int i = 1; // increment days
            while (!cur_literal.equals(date_literal)) {
                cal.setTimeInMillis(timestamp);
                cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + i++);
                date_literal = extractYearMonthDayLiteral(cal.getTimeInMillis());
                res.add(date_literal);
            }
        }
        return res;
    }
}
