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
package gr.demokritos.iit.location.repository;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.structs.LocSched;
import gr.demokritos.iit.location.util.GeometryFormatTransformer;

import java.io.IOException;
// dependencies of json POST
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import java.util.*;

/**
 * handles the persistence of items (i.e. articles/tweets) updated with referrals to places.
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocationCassandraRepository extends BaseCassandraRepository implements ILocationRepository {

    private static final String SCHEDULE_TYPE_BASE = "location_extraction";

    public LocationCassandraRepository(Session session) {
        super(session);
    }

    @Override
    public LocSched scheduleInitialized(OperationMode mode) {
        String schedule_type = new StringBuilder().append(SCHEDULE_TYPE_BASE).append("_").append(mode.getMode()).toString();
        Statement select = QueryBuilder
                .select(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName(), Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName())
                .from(session.getLoggedKeyspace(), Cassandra.Location.Tables.LOCATION_LOG.getTableName())
                .where(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), schedule_type)).limit(1);
        ResultSet results = session.execute(select);

        long max_existing = 0l;
        // set initial last 2 months ago
        Calendar two_months_ago = Calendar.getInstance();
        two_months_ago.set(Calendar.MONTH, two_months_ago.get(Calendar.MONTH) - 2);
        long last_parsed = two_months_ago.getTimeInMillis();

        Row one = results.one();
        if (one != null) {
            max_existing = one.getLong(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName());
            last_parsed = one.getLong(Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName());
        }
        long current = max_existing + 1;
        LocSched curSched = new LocSched(mode, current, last_parsed);

        Statement insert = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Cassandra.Location.Tables.LOCATION_LOG.getTableName())
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), schedule_type)
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName(), current)
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_END.getColumnName(), 0l) // avoid nulls
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_ITEMS_UPDATED.getColumnName(), 0l) // avoid nulls
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName(), last_parsed)
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_START.getColumnName(), new Date().getTime());
        session.execute(insert);
        return curSched;
    }

    @Override
    public void scheduleFinalized(LocSched sched) {
        String schedule_type = new StringBuilder().append(SCHEDULE_TYPE_BASE).append("_").append(sched.getOperationMode().getMode()).toString();
        Statement update = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.Location.Tables.LOCATION_LOG.getTableName())
                .with(set(Cassandra.Location.TBL_LOCATION_LOG.FLD_END.getColumnName(), new Date().getTime()))
                .and(set(Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName(), sched.getLastParsed()))
                .and(set(Cassandra.Location.TBL_LOCATION_LOG.FLD_ITEMS_UPDATED.getColumnName(), sched.getItemsUpdated()))
                .where(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), schedule_type))
                .and(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName(), sched.getScheduleID()));
        session.execute(update);
    }


    @Override
    public void updateArticlesWithReferredPlaceMetadata(String permalink, Map<String, String> places_polygons) {
        System.out.println(String.format("\tupdating news tables, article: %s with places: %s", permalink, places_polygons.keySet().toString()));
        // load metadata
        Map<String, Object> article = loadArticle(permalink);
        long published = (long) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName());
        String ymdl = Utils.extractYearMonthDayLiteral(published);
        String reversed_host = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName());
        Set<String> places = places_polygons.keySet();
        // update news_articles
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES.getTableName())
                .with(set(Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName(), places))
                .where(eq(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), permalink))
                .and(eq(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(), reversed_host));
        session.execute(upsert);
        // update news_articles_per_published_date
        upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                .with(set(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), places))
                .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), permalink))
                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), ymdl))
                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), published));
        session.execute(upsert);
        // update news_articles_per_crawled_date
        long crawled = (long) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName());
        upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_CRAWLED_DATE.getTableName())
                .with(set(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), places))
                .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), permalink))
                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(),
                                Utils.extractYearMonthDayLiteral(crawled)))
                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(), crawled));
        session.execute(upsert);
        Statement insert;
        for (String place : places) {
            insert = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PLACE.getTableName())
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), place)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.getColumnName(), permalink)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PUBLISHED.getColumnName(), published)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName(), places_polygons.get(place))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_FEED_URL.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_FEED_URL.getColumnName()))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CRAWL_ID.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWL_ID.getColumnName()))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_RAW_TEXT.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_RAW_TEXT.getColumnName()))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CLEAN_TEXT.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CLEAN_TEXT.getColumnName()))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CRAWLED.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName()))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_LANGUAGE.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_LANGUAGE.getColumnName()))
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_TITLE.getColumnName(), article.get(Cassandra.RSS.TBL_ARTICLES.FLD_TITLE.getColumnName()));
            session.execute(insert);

        }
        // this populates existing events table with locations/polygons pairs from news
        //updateEventsWithArticleLocationPolygonPairs(places_polygons, permalink);
    }

    @Override
    public void updateTweetsWithReferredPlaceMetadata(long post_id, Map<String, String> places_polygons) {
        if (places_polygons == null || places_polygons.isEmpty()) {
            return;
        }
        System.out.println("\tupdating twitter table" + Cassandra.Twitter.Tables.TWITTER_POSTS_PER_REFERRED_PLACE.getTableName() + ", tweet: " + post_id + ", with: " + places_polygons.keySet().toString());
        // load tweet from repository
        Map<String, Object> tweet = loadTweet(post_id);
        // update twitter post with referred place
        Statement insert;
        for (Map.Entry<String, String> entry : places_polygons.entrySet()) {
            String place = entry.getKey();
            String coordinates = entry.getValue();
            // insert metadata
            insert = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_REFERRED_PLACE.getTableName())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_PLACE_LITERAL.getColumnName(), place)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_CREATED_AT.getColumnName(), tweet.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_CREATED_AT.getColumnName()))
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_POST_ID.getColumnName(), post_id)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_LOCATION.getColumnName(), coordinates)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_TWEET.getColumnName(), tweet.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_TWEET.getColumnName()))
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_LANGUAGE.getColumnName(), tweet.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName()))
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_PERMALINK.getColumnName(), tweet.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PERMALINK.getColumnName()))
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_ACCOUNT_NAME.getColumnName(), tweet.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_ACCOUNT_NAME.getColumnName()));
            session.execute(insert);
        }
        // this populates existing events table with locations/polygons pairs from tweets
        //updateEventsWithTweetLocationPolygonPairs(places_polygons,post_id);
    }


    @Override
    public Map<String, Object> loadArticlePerPlace(String place_literal, String entry_url) {
        ResultSet results;
        Statement select = QueryBuilder
            .select()
            .all()
            .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PLACE.getTableName())
            .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), place_literal))
            .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.getColumnName(), entry_url))
            // we expect only one entry
            .limit(1);
        results = session.execute(select);
        Map<String, Object> res = new HashMap();
        for (Row row : results) {
            String place = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), place);
            String url = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.getColumnName(), url);
            long published = row.getLong(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PUBLISHED.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PUBLISHED.getColumnName(), published);
            String bounding_box = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName(), bounding_box);
            String feed_url = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_FEED_URL.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_FEED_URL.getColumnName(), feed_url);
            long crawl_id = row.getLong(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CRAWL_ID.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CRAWL_ID.getColumnName(), crawl_id);
            String raw_text = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_RAW_TEXT.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_RAW_TEXT.getColumnName(), raw_text);
            String clean_text = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CLEAN_TEXT.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CLEAN_TEXT.getColumnName(), clean_text);
            long crawled = row.getLong(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CRAWLED.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_CRAWLED.getColumnName(), crawled);
            String lang = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_LANGUAGE.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_LANGUAGE.getColumnName(), lang);
            String title = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_TITLE.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_TITLE.getColumnName(), title);
        }
        return Collections.unmodifiableMap(res);
    }


    void updateEventsWithArticleLocationPolygonPairs(Map<String,String> places_polygons, String permalink)
    {
        //System.out.println("\t>Updating events with article permalink: " + permalink); //debugprint
        long startTime = System.currentTimeMillis();
        Set<String> places = places_polygons.keySet();
        // this is an ugly workaround. a table events per article would be superb

        // cheaper to go per article ? per place ? per event?
        // probably per event.

        // get all events
        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        Set<String> eventIDs = new HashSet<String>();
        for(Row row : results)
        {

            eventIDs.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));
        }
        String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(places_polygons);

        PreparedStatement pstatement = session.prepare(
                "UPDATE " + session.getLoggedKeyspace() +"." + Cassandra.Event.Tables.EVENTS.getTableName()
                +" SET " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " = " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " + "
                + payload + " WHERE " + Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName() + " = ? "
        );
        BoundStatement bstatement = new BoundStatement(pstatement);


        // for each event
        for(String event  : eventIDs)
        {
            // get its source urls with a cql query
            query = QueryBuilder
                    .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName())
                    .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName())
                    .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),event));
            results = session.execute(query);

            // for each article url
            for(Row row : results)
            {

                Set<String> articleURLs = row.getSet(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(),String.class);
                if (!articleURLs.contains(permalink)) continue;

                else
                {
                    // insert the place mappings in that event
                    System.out.println("\t\t>>> Inserting to event " + event + " , places :" + places.toString());
                    session.execute(bstatement.bind(event));
                    break;
                }



            }
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        //System.out.println("### Done with article permalink: " + permalink + " in " + Long.toString(duration) + " msec");

    }
    // hotfix for populating events table
    void updateEventsWithTweetLocationPolygonPairs(Map<String,String> places_polygons, long post_id)
    {
        //String strpostid = Long.toString(post_id);
        //System.out.println("\t>Updating events with tweet post id: " + strpostid); //debugprint

        long startTime = System.currentTimeMillis();
        Set<String> places = places_polygons.keySet();
        // this is an ugly workaround. a table events per article would be superb

        // cheaper to go per article ? per place ? per event?
        // probably per event.

        // get all events
        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        Set<String> eventIDs = new HashSet<String>();
        for(Row row : results)
        {

            eventIDs.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));
        }
        String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(places_polygons);

        PreparedStatement pstatement = session.prepare(
                "UPDATE " + session.getLoggedKeyspace() +"." + Cassandra.Event.Tables.EVENTS.getTableName()
                        +" SET " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " = " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " + "
                        + payload + " WHERE " + Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName() + " = ? "
        );
        BoundStatement bstatement = new BoundStatement(pstatement);


        // for each event
        for(String event_id  : eventIDs)
        {
            // get its source urls with a cql query
            query = QueryBuilder
                    .select(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName())
                    .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName())
                    .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),event_id));
            results = session.execute(query);

            // for each article url
            for(Row row : results)
            {

                Set<Long> tweet_ids = row.getSet(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(),Long.class);
                if (!tweet_ids.contains(post_id)) continue;

                else
                {
                    // insert the place mappings in that event
                    System.out.println("\t\t>>> Inserting to event " + event_id + " , places :" + places.toString());
                    session.execute(bstatement.bind(event_id));
                    break;
                }



            }
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        //System.out.println("### Done with article permalink: " + strpostid + " in " + Long.toString(duration) + " msec");

    }

    /**
     * Function to send events to popeye.di.uoa.gr for storage and/or change detection
     */
    @Override
    public void storeAndChangeDetectionEvents()
    {
        // get all events, fields: id, title, date, placemappings
        // store each field of interest in an arraylist
        ArrayList<String> entries = new ArrayList<>();
        // perform the query
        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        int count = 1;
        // for each event
        for(Row row : results)
        {
            entries.clear();
            // get the id
            entries.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));

            // place-mappings: If null, skip the processing
            Map<String,String> locpoly = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(),String.class,String.class);
            if (locpoly.isEmpty())
            {
                System.out.println("Skipping processing event " + count++ + ":" + entries.get(entries.size()-1) + " due to no assigned geometries." );
                entries.clear();
                continue;
            }

            // get the title
            entries.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName()));
            // get date
            entries.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName()));
            // get  geometry
            for(String item : locpoly.keySet())
            {
                String val = locpoly.get(item);
                entries.add(item);
                entries.add(val);
            }
            // reconstruct the entries in the format expected by popeye
            String payload = GeometryFormatTransformer.EventRowToPopeyeProcess(entries);
            System.out.println("payload is:" + payload); // debugprint

            // send http request
            // TODO: make new connection for each send or make connection out of loop?
            // TODO: put target url in the properties file on the module this function will
            // end up in
            String URLstr = "http://popeye.di.uoa.gr:8080/changeDetection/event/process";
            URL url;
            HttpURLConnection connection = null;
            try
            {
                // open connection, set JSONic properties
                url = new URL(URLstr);
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
                String resp = response.toString();
                rd.close();
                // debugprint
                System.out.println("popeye response:\n\t" + resp);

                if (resp.equals("{\"code\":400,\"message\":\"exception\"}"))
                {
                    System.err.println("Popeye request failed.");
                }
            }
            catch(MalformedURLException exc)
            {
                System.err.println("Malformed event processing URL:\n\t" + URLstr);
                return;
            }
            catch(IOException exc)
            {
                System.err.println("IO error during event processing connection initialization:\n");
                System.err.println(exc.getMessage());
                System.err.println(exc.toString());
                exc.printStackTrace();
                return;
            }
            finally
            {
                if(connection != null)
                    connection.disconnect();

            }


        }

    }
}
