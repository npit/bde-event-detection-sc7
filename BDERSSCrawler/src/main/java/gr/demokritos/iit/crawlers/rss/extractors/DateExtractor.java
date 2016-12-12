package gr.demokritos.iit.crawlers.rss.extractors;

import org.apache.commons.lang3.ObjectUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by npittaras on 24/11/2016.
 */

/**
 * Class to extract date from a html
 */
public class DateExtractor {

    static String[] dateAttributes = {"article:published_time", "article:modified_time"};

    static String[] dateformats = {"yyyy-MM-dd'T'HH:mm:ssX"};

    private static Date error()
    {
        System.err.print("\tCould not resolve article date: ");
        System.err.println("\tUsing <now> as a fallback.");

        return new Date(System.currentTimeMillis());
    }
    public static Date getDate(Document doc,String url) {
        Date articleDate = null;
        String datestr="";
        try {
            datestr = DateExtractor.getDateString(doc, url);
        }
        catch(NullPointerException ex)
        {
            System.err.println("Failed to extract date : " + ex.getMessage());
            return error();
        }
        if(datestr == null || datestr.isEmpty())
        {
            System.err.println("\tNo date string found in article source.");
            return error();
        }
        articleDate = DateExtractor.parseDateFromString(datestr);
        // attempts to get publish date failed. manual parsing required
        if(articleDate == null)
            return error();
        return articleDate;
    }
    private static Date parseDateFromString(String DateString)
    {
        Date articleDate = null;
        // parse unix timestamp
        try
        {
            long ts = Long.parseLong(DateString);
            return new Date(ts);
        }
        catch(NumberFormatException ex)
        {
            ;
        }

        // parse date string
        for(String dateformat : dateformats)
        {
            try
            {
                DateFormat format = new SimpleDateFormat(dateformat);
                System.out.println(format.format(new Date(System.currentTimeMillis())));

                articleDate  = format.parse(DateString);
                if (articleDate != null)
                {
                    return articleDate;
                }
            } catch (ParseException e) {
                continue;
            }
        }
        return null;

    }
    private static String getDateString(Document doc, String urladdress) throws NullPointerException {

        // try with jsoup
        Element head = doc.head();
        boolean DateStringAcquired = false;
        boolean DateParsed = false;
        String DateString = "";
        Elements metas = head.select("meta[property]");

        for (String attr : dateAttributes) {

            for (Element meta : metas) {

                Elements pr = meta.getElementsByAttributeValue("property", attr);
                if (!pr.isEmpty()) {
                    if (pr.size() > 1)
                        System.err.println("More than one date header meta tag with [" + attr + "] property. Using the first.");
                    Element el = pr.get(0);
                    DateString = el.attr("content");
                    return DateString;
                }
            }
        }

        // try with the last-modified header

        // try with html connection
        // quite unreliable
        try {
            URL url = new URL(urladdress);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            long unixdate = httpCon.getLastModified();

            if(unixdate != 0)
            {
                Date test = new Date(unixdate);
                if(test != null)
                    return Long.toString(unixdate);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "";
    }
}
