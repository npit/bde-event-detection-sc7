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
package gr.demokritos.iit.crawlers.twitter.repository;

import com.twitter.Extractor;
import java.util.ArrayList;
import java.util.List;
import gr.demokritos.iit.crawlers.twitter.url.DefaultURLUnshortener;
import gr.demokritos.iit.crawlers.twitter.url.IURLUnshortener;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import twitter4j.GeoLocation;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public abstract class AbstractRepository {

    public static final String TWEET_UNDEFINED_LANG = "und";
    protected Extractor extractor;
    protected IURLUnshortener unshortener;

    private final StringBuilder sb;

    /**
     *
     * @param unshortenerArg the unshortener
     */
    public AbstractRepository(IURLUnshortener unshortenerArg) {
        // init twitter extractor
        this.extractor = new Extractor();
        // init DefaultURLUnshortener
        this.unshortener = unshortenerArg;
        this.sb = new StringBuilder();
    }

    /**
     * Uses default params for URL unshortener
     *
     */
    public AbstractRepository() {
        // init twitter extractor
        this.extractor = new Extractor();
        // init DefaultURLUnshortener
        this.unshortener = new DefaultURLUnshortener();
        this.sb = new StringBuilder();
    }

    /**
     * Calls {@link DefaultURLUnshortener} to expand shortened URLs
     *
     * @param lsURLs the list of URLs contained in a tweet
     * @return a list of unshortened URLs
     */
    protected List<String> unshortenURLs(List<String> lsURLs) {
        List<String> lsRes = new ArrayList();
        for (String tCo : lsURLs) {
            lsRes.add(unshortener.expand(tCo));
        }
        return lsRes;
    }

    protected String extractYearMonthLiteral(Date date) {
        if (date == null) {
            return "";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        return new StringBuilder(year).append("-").append(month).toString();
    }

    /**
     *
     * @param geolocation
     * @return a '[+-]XX_[+-]YY' representation of the coordinates, i.e. keep
     * digits before the dot.
     */
    protected String extractGeolocationLiteral(GeoLocation geolocation) {
        if (geolocation == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        double lat = geolocation.getLatitude();
        double lon = geolocation.getLongitude();
        String sLat = String.valueOf(lat);
        String sLon = String.valueOf(lon);

        String first = getSubText(sLat, geo_regex, 1);
        String second = getSubText(sLon, geo_regex, 1);

        sb.append(first).append("_").append(second);
        return sb.toString();
    }
    protected String geo_regex = "([+-]*[0-9]+)[.][0-9]+";
    protected String tco_regex = "http[s]*://t.co";

    private String getSubText(String sContent, String sRegex, int i) {
        Matcher m = Pattern.compile(sRegex).matcher(sContent);
        if (m.find()) {
            return m.group(i);
        }
        return "";
    }

    protected String cleanTweetFromURLs(Status post) {
        URLEntity[] urlEntities = post.getURLEntities();
        String tweet = post.getText();
        for (URLEntity urlEntity : urlEntities) {
            String url1 = urlEntity.getURL();
            String url2 = urlEntity.getDisplayURL();
            tweet = tweet.replace(url1, "");
            tweet = tweet.replace(url2, "");
        }
        MediaEntity[] mediaEntities = post.getMediaEntities();
        for (MediaEntity mediaEntity : mediaEntities) {
            String ent = mediaEntity.getURL();
            tweet = tweet.replace(ent, "");
        }
        return tweet;
    }

    /**
     *
     * @param post
     * @return the External URL entities from the post (not media entities)
     */
    protected List<String> getURLEntities(Status post) {
        List<String> res = new ArrayList();
        URLEntity[] urlEntities = post.getURLEntities();
        for (URLEntity urlEntity : urlEntities) {
            res.add(urlEntity.getURL());
        }
        return res;
    }

    protected String extractTweetPermalink(String account_name, Long post_id) {
        try {
            sb.append("https://twitter.com/").append(account_name).append("/status/").append(post_id);
            return sb.toString();
        } finally {
            sb.delete(0, sb.length());
        }
    }
}
