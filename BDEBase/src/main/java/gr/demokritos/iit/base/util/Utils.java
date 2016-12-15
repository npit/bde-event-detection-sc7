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

import org.apache.commons.lang3.time.StopWatch;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class Utils {

    public static boolean checkResponse(String resp)
    {
        if (resp.equals("{\"code\":400,\"message\":\"exception\"}") || resp.isEmpty())
        {
            System.err.println("Server request failed.");
            return false;
        }
        return true;
    }

    public static String sendGET(String address) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        if(conn!=null) conn.disconnect();
        return result.toString();
    }
    public static String sendPOST(String payload, String address)
    {
        URL url;
        HttpURLConnection connection = null;
        String resp = "";
        try
        {
            // open connection, set JSONic properties
            url = new URL(address);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setRequestProperty("Accept","application/json");
            connection.setRequestProperty("Content-Length",
                    Integer.toString(payload.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(payload);
            wr.close();
            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            // parse to string
            StringBuilder response = new StringBuilder(); // or StringBuffer if not Java 5+
            String line;
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            resp = response.toString();
            rd.close();
            //System.out.println("server response:\n\t" + resp);
        }
        catch(MalformedURLException exc)
        {
            System.err.println("Malformed event processing URL:\n\t" + address);
        }
        catch(IOException exc)
        {
            System.err.println("IO error during event processing connection initialization:\n");
            System.err.println(exc.getMessage());
            System.err.println(exc.toString());
            exc.printStackTrace();
        }
        finally
        {
            if(connection != null)
                connection.disconnect();
        }
        return resp;
    }
    private static Stack<Long> tictoc;
    public static void tic()
    {


        if(tictoc == null) tictoc = new Stack<>();
        tictoc.push(System.nanoTime());

    }
    public static String toc()
    {
        if(tictoc == null) return "[tictoc not initialized]";
        if(tictoc.isEmpty()) return "[empty tictoc]";
        Long timenow = System.nanoTime();
        Long timethen = tictoc.pop();

        return Long.toString((timenow-timethen)/1000l) + " sec ";
    }
    public static void tocTell()
    {
        tocTell("");
    }
    public static void tocTell(String operation)
    {
        System.out.println("Tic-toc for [" + operation + "]  : " + toc());

    }
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
        if (timestamp == 0l || timestamp == Long.MIN_VALUE) {
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
            if (timestamp == 0l || timestamp == Long.MIN_VALUE) {
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

    /**
     *
     * @param window : specifier for time window to extract articles/tweets from
     *               <integer>_<days/months/hours/years>
     * @return
     */
    public static Calendar getCalendarFromStringTimeWindow(String window)
    {
        // default : 1 month
        Calendar cal = Calendar.getInstance();

        if (window.isEmpty())
        {
            System.err.println("No time window specified, using default 1 month window.");
            cal.set(Calendar.MONTH,cal.get(Calendar.MONTH )-1);
            return cal;
        }
        System.out.println("Using document retrieval window : [" + window + "]");
        String [] tokens = window.split("_");
        int offset = Integer.parseInt(tokens[0]);
        tokens[1] = tokens[1].toLowerCase();
        if(tokens[1].equals("months") || tokens[1].equals("month"))
        {
            cal.set(Calendar.MONTH,cal.get(Calendar.MONTH ) - offset);
        }
        else if(tokens[1].equals("weeks") || tokens[1].equals("week"))
        {
            cal.set(Calendar.DAY_OF_MONTH,cal.get(Calendar.DAY_OF_MONTH ) - 7*offset);
        }
        else if(tokens[1].equals("days") || tokens[1].equals("day"))
        {
            cal.set(Calendar.DAY_OF_MONTH,cal.get(Calendar.DAY_OF_MONTH ) - offset);
        }
        else if(tokens[1].equals("years") || tokens[1].equals("year"))
        {
            cal.set(Calendar.YEAR,cal.get(Calendar.YEAR ) - offset);
        }
        else
        {
            System.err.println("Unspecified time window type [" + tokens[1] + "].");
            System.err.println("Available are [days weeks months years]");
            System.err.println("Using default 1 month window.");

            cal.set(Calendar.MONTH,cal.get(Calendar.MONTH ) -1);

        }

        return cal;

    }
//    public static String unixTimeToDateString(long timestamp)
//    {
//        Date date = new Date(timestamp*1000L); // *1000 is to convert seconds to milliseconds
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss z"); // the format of your date
//        //sdf.setTimeZone(TimeZone.getTimeZone("GMT-4")); // give a timezone reference for formating (see comment at the bottom
//        String formattedDate = sdf.format(date);
//        return (formattedDate);
//    }
    public static String cleanTweet(String tweet) {
        // apart from noise removal, do we need anything else?
        return removeNoise(tweet);
    }


    public static String toTimezoneFormattedStr(Calendar date, String timezoneID, String outputDateFormat) {
        Date dDate = new Date(date.getTimeInMillis());
        return toTimezoneFormattedStr(dDate, timezoneID, outputDateFormat);
    }

    public static String toTimezoneFormattedStr(Date date, String timezoneID, String outputDateFormat) {
        TimeZone tz = TimeZone.getTimeZone(timezoneID);
        DateFormat df = new SimpleDateFormat(outputDateFormat);
        df.setTimeZone(tz);
        return df.format(date);
    }


    // reused from SocialMediaEvaluator-BDE-SNAPSHOT
    private static String removeNoise(String target) {
        target = target.replaceAll("((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", ""); // links
        target = target.replaceAll("(((RT )|(MT ))?@([\\w]|[^\\s])*([\\s]|$))+", "");                                                     //RT MT @someone (response to)
        target = target.replaceAll("@([\\w]|[^\\s])*([\\s]|$)", " ");                                                                     //@someone
        target = target.replaceAll("&[\\w:]+;", "");                                                                                      //&gt; etc
        target = target.replaceAll("[^#$\\w\\s]+", " ");                                                                                  //'...,,,**^^^()'
        target = target.replaceAll("[\\s]http([\\s]|$)", " ");                                                                            //' http '
        return target;
    }
// a few test cases
//    public static void main(String[] args) {
//        String tweet1 = "This is from Petaflop Choreography - a poem handed to me by Shirley Sampson in Glasgow, Saturday... #WorldPoetryDay ";
//        String tweet2 = "Powa failure - @BBCRoryCJ on how a UK start-up that claimed to have \"trumped\" Apple fell to earth ";
//        String tweet3 = "Obama is the first U.S. president to visit Cuba since Calvin Coolidge arrived by sea in 1928 http://usat.ly/25fcS6Z  ";
//        String tweet4 = "Thousands of mentally ill Indonesians are living in shackles, a new report says http://ti.me/1ZjdPqL  ";
//        String tweet5 = "Novak Djokovic controversially said male tennis players should be paid more than women http://trib.al/M9v9Nh5  ";
//        String tweet6 = "@SkyNews Some men need to know when to keep their mouths SHUT!!!!";
//        List<String> tweets = new ArrayList();
//        tweets.add(tweet1);
//        tweets.add(tweet2);
//        tweets.add(tweet3);
//        tweets.add(tweet4);
//        tweets.add(tweet5);
//        tweets.add(tweet6);
//
//        for (String tw :
//                tweets) {
//            System.out.println(tw);
//            System.out.println(cleanTweet(tw));
//        }
//
//    }
}
