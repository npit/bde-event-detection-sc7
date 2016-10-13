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
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import com.vividsolutions.jts.io.ParseException;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.structs.LocSched;
import gr.demokritos.iit.location.util.GeometryFormatTransformer;
import org.omg.CORBA.DynAnyPackage.Invalid;

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

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

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
    public LocSched scheduleInitialized(OperationMode mode)
    {
        java.util.Calendar window = Calendar.getInstance();
        return scheduleInitialized(mode,window);
    }
        @Override
    public LocSched scheduleInitialized(OperationMode mode,java.util.Calendar window) {
        String schedule_type = new StringBuilder().append(SCHEDULE_TYPE_BASE).append("_").append(mode.getMode()).toString();
        Statement select = QueryBuilder
                .select(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName(), Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName())
                .from(session.getLoggedKeyspace(), Cassandra.Location.Tables.LOCATION_LOG.getTableName())
                .where(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), schedule_type)).limit(1);
        ResultSet results = session.execute(select);

        long max_existing = 0l;
        // replaced with argument
        // set initial last 2 months ago
//        Calendar two_months_ago = Calendar.getInstance();
//        System.out.println("*****************SETTING 1 year as time window");
//        two_months_ago.set(Calendar.MONTH, two_months_ago.get(Calendar.MONTH) - 1);
        long last_parsed = window.getTimeInMillis();
        System.out.println("Data retrieval window set from now to :" + new Date(last_parsed).toString());

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
        System.out.println(insert.toString());
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
        System.out.println(String.format("\tupdating with places: %s", places_polygons.keySet().toString()));
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
        // this populates existing events table with locations/polygons pairs from news. It's a hotfix
        //updateEventsWithArticleLocationPolygonPairs(places_polygons, permalink);
    }

    @Override
    public void updateTweetsWithReferredPlaceMetadata(long post_id, Map<String, String> places_polygons) {
        if (places_polygons == null || places_polygons.isEmpty()) {
            return;
        }
        System.out.println(" - updating with places: " + places_polygons.keySet().toString());
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
        // this populates existing events table with locations/polygons pairs from tweets. It's a hotfix
        //updateEventsWithTweetLocationPolygonPairs(places_polygons,post_id);
    }

    @Override
    public void updateEventsWithArticleLocationPolygonPairs(Map<String,String> places_polygons, String permalink)
    {
        System.out.println("\t>Updating events with places of article permalink: " + permalink); //debugprint
        long startTime = System.currentTimeMillis();
        Set<String> places = places_polygons.keySet();
        // this is an ugly workaround. a table events per article would be superb

        // cheaper to go per article ? per place ? per event?
        // probably per event.

        // get all event ids
        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        Set<String> eventIDs = new HashSet<String>();
        for(Row row : results)
        {

            eventIDs.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));
        }

        // get all locationName - geometry pairs. Iterate through each because putting all of them in a query
        // might invoke an invalid query exception due to a size limit on insertions on a map column
        for(String locationName : places_polygons.keySet()) {
            //System.out.println("Preparing with " + locationName);

            //String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(places_polygons);
            Map<String,String> singlePair = new HashMap<>();
            singlePair.put(locationName,places_polygons.get(locationName));
//            String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(singlePair);
//            System.out.println("Location-geometry payload for event is " + payload);
//            System.out.println("Location-geometry payload for event is " + payload.length() + " chars.");
//            System.out.println("Location-geometry payload for event is " + payload.getBytes().length + " bytes.");
//            PreparedStatement pstatement = session.prepare(
//                    "UPDATE " + session.getLoggedKeyspace() + "." + Cassandra.Event.Tables.EVENTS.getTableName()
//                            + " SET " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " = " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " + "
//                            + payload + " WHERE " + Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName() + " = ? "
//            );
//            System.out.println(payload);
//
//            BoundStatement bstatement = new BoundStatement(pstatement);


            // for each event
            for (String event : eventIDs) {
                // get its source urls with a cql query
                query = QueryBuilder
                        .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName())
                        .from(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                        .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), event));
                results = session.execute(query);

                // for each article url
                for (Row row : results) {
                    Map<String, String> articleURLs = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), String.class, String.class);
                    Set<String> justURLS = articleURLs.keySet();
                    //                System.err.println("DEBUG - force-inserting article to event " + event );
                    if (!justURLS.contains(permalink)) continue;
                    else {
                        // insert the place mappings in that event
                        System.out.println("\t\t>>> Inserting to event " + event + " , place :" + singlePair.keySet());

                        Statement update = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                                .with(QueryBuilder.put(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), locationName,singlePair.get(locationName)))
                                .where(QueryBuilder.eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), event));


                        session.execute(update);

                        //session.execute(bstatement.bind(event));
                        break;
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        //System.out.println("### Done with article permalink: " + permalink + " in " + Long.toString(duration) + " msec");

    }
    // hotfix for populating events table
    @Override
    public void updateEventsWithTweetLocationPolygonPairs(Map<String,String> places_polygons, long post_id)
    {
        String strpostid = Long.toString(post_id);
        System.out.println("\t>Updating events with places of tweet post id: " + strpostid); //debugprint

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
        for(String locationName : places_polygons.keySet()) {

            //String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(places_polygons);
            //System.out.println("Preparing with " + locationName);
            Map<String, String> singlePair = new HashMap<>();
            singlePair.put(locationName, places_polygons.get(locationName));
//            String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(singlePair);
//
////            System.out.println("\t>Converting to CQL: " + strpostid); //debugprint
////            String payload = GeometryFormatTransformer.LocationPolygonsToCQLString(places_polygons);
////            System.out.println("\t>Converted: " + payload); //debugprint
//            PreparedStatement pstatement = session.prepare(
//                    "UPDATE " + session.getLoggedKeyspace() + "." + Cassandra.Event.Tables.EVENTS.getTableName()
//                            + " SET " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " = " + Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName() + " + "
//                            + payload + " WHERE " + Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName() + " = ? "
//            );
//            BoundStatement bstatement = new BoundStatement(pstatement);
//            System.out.println("\t>Made bound statement: " + strpostid); //debugprint

            // for each event
            for (String event_id : eventIDs) {
                //System.out.println("\t>Checking event id: " + event_id + " . Tweets"); //debugprint

                // get its source urls with a cql query
                query = QueryBuilder
                        .select(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName())
                        .from(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                        .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), event_id));
                results = session.execute(query);

                // for each article url
                for (Row row : results) {
                    //System.out.println("\t>tweet id: " + post_id); //debugprint

                    Map<Long, String> tweet_ids = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(), Long.class, String.class);
                    Set<Long> justIDs = tweet_ids.keySet();
                    if (!justIDs.contains(post_id)) continue;

                    else {
                        // insert the place mappings in that event
                        //System.out.println("\t\t>>> Inserting to event " + event_id + " , places :" + places.toString());
                        System.out.println("\t\t>>> Inserting to event " + event_id + " , place :" + singlePair.keySet());

                        Statement update = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                                .with(QueryBuilder.put(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), locationName,singlePair.get(locationName)))
                                .where(QueryBuilder.eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), event_id));


                        session.execute(update);
//                        session.execute(bstatement.bind(event_id));
                        break;
                    }


                }
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        //System.out.println("### Done with article permalink: " + strpostid + " in " + Long.toString(duration) + " msec");

    }

    /**
     * Update events table with location data from news and tweets. Location data will be fetched from the respective tables,
     * not computed.
     */
    @Override
    public void onlyUpdateEventsWithLocationInformation()
    {
        // get all event ids, articles per event, tweets per event
        System.out.println("Loading event data.");

        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        ArrayList<String> eventIDs = new ArrayList<>();
        ArrayList<ArrayList<String>> permalinksPerEvent = new ArrayList<>();
        ArrayList<ArrayList<Long>> tweetsPerEvent = new ArrayList<>();

        for(Row row : results)
        {
            eventIDs.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));

            ArrayList<String> list = new ArrayList<>();

            Map<String,String> articles_queryData = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(),String.class,String.class);
            list.addAll(articles_queryData.keySet());
            permalinksPerEvent.add(list);
            Map<Long,String> tweets_queryData = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(),Long.class,String.class);
            ArrayList<Long> listlong = new ArrayList<>();

            listlong.addAll(tweets_queryData.keySet());
            tweetsPerEvent.add(listlong);
        }

        System.out.println("Loading article data.");

        /*
        Get article information:
          Get all primary keys (entry_url, reversed_host) and place_literal list
         */
        query = QueryBuilder
                .select(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(),
                        //Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(),
                        Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.RSS.Tables.NEWS_ARTICLES.getTableName());
        results = session.execute(query);

        ArrayList<String> urls = new ArrayList<>();
        ArrayList<ArrayList<String>>  url_places = new ArrayList<>();


        for (Row row : results)
        {
            String url = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName());
            ArrayList<String> places = new ArrayList<>();
            places.addAll(row.getSet(Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName(),String.class));

            urls.add(url);
            url_places.add((ArrayList)places);
        }
        System.out.println("Loaded " + urls.size() + " distinct urls.");

        /*
            Get tweets information. We have to get quite a bit of stuff due to the primary key
            fields of twitter posts per referred place


         */
        System.out.println("Loading twitter data.");

        query = QueryBuilder
                .select(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_PLACE_LITERAL.getColumnName(),
                        Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_POST_ID.getColumnName(),
                        Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_CREATED_AT.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Twitter.Tables.TWITTER_POSTS_PER_REFERRED_PLACE.getTableName());
        results = session.execute(query);

        ArrayList<Long> post_ids = new ArrayList<>();
        ArrayList<ArrayList<String>> tweet_places = new ArrayList<>();
        ArrayList<ArrayList<Long>> created_at = new ArrayList<>();
        // populate
        int count = 0;
        for (Row row : results)
        {
            ++count;
            String place = row.getString( Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_PLACE_LITERAL.getColumnName());
            Long postid = row.getLong( Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_POST_ID.getColumnName());
            Long createdat = row.getLong( Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_CREATED_AT.getColumnName());

            if(post_ids.contains(postid))
            {
                // append to existing
                int index = post_ids.indexOf(postid);
                tweet_places.get(index).add(place);
                created_at.get(index).add(createdat);
            }
            else
            {
                // appent both new
                post_ids.add(postid);
                tweet_places.add(new ArrayList<String>());
                created_at.add(new ArrayList<Long>());
                tweet_places.get(tweet_places.size()-1).add(place);
                created_at.get(created_at.size()-1).add(createdat);
            }
        }
        System.out.println("Loaded " + count + " total tweet-place pairs," + post_ids.size() + " distinct tweets.");
        HashMap<String,String> placesCache = new HashMap<>();
        // update the events. For each event
        System.out.println("Populating events.");
        for(int ev=0;ev<eventIDs.size();++ev)
        {
            String eventID = eventIDs.get(ev);

            System.out.println(String.format("***Populating event %d/%d : [%s]",ev,eventIDs.size(),eventID));
            if(permalinksPerEvent.get(ev).isEmpty()) System.out.println("No articles");
            // for each article in the event
            count =1;
            for(String article : permalinksPerEvent.get(ev))
            {
                System.out.print(String.format("\tpermalink  %d/%d : [%s] {",count++,permalinksPerEvent.get(ev).size(),article));
                // index of article
                int artidx = urls.indexOf(article);
                if(artidx < 0)
                {
                    System.out.print(String.format("(!) %s was not found (no geom/place)! }\n",article));
                    continue;
                }
                if( url_places.get(artidx).isEmpty()) System.out.print("No places.");
                for(String place : url_places.get(artidx))
                {
                    String geometry = "";
                    if (placesCache.containsKey(place)) {
                        geometry = placesCache.get(place);
                        System.out.print(" c");
                    }
                    else
                    {
                        // query it
                        query = QueryBuilder
                                .select(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName())
                                .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PLACE.getTableName())
                                .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), place))
                                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.getColumnName(), article));

                        results = session.execute(query);
                        for (Row row : results) {
                            String bbox = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName());
                            placesCache.put(place, bbox);
                            geometry = bbox;
                        }
                        System.out.print(" q");

                    } // else query it

                    // insert
                    Statement upd = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                            .with(put(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), place, geometry))
                            .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), eventID));
                    try
                    {
                        session.execute(upd);
                    }
                    catch(InvalidQueryException ex)
                    {
                        System.err.print("query-failed:("+place+")");
                    }

                    System.out.print("[" + place+"]");

                } // for each article place
                System.out.println(" }");
            } // for each article

            count =1;
            if(tweetsPerEvent.get(ev).isEmpty()) System.out.println("\tNo tweets");
            for(Long tweet : tweetsPerEvent.get(ev))
            {
                System.out.print(String.format("tweet  %d/%d : [%d] {",count++,tweetsPerEvent.get(ev).size(),tweet));
                // index of article
                int tweetidx = post_ids.indexOf(tweet);
                if(tweetidx < 0)
                {
                    System.out.print(String.format("(!)  %d  not found (no geom/place)! }\n",tweet));
                    continue;
                }

                for( int tweet_inner_idx=0; tweet_inner_idx < tweet_places.get(tweetidx).size(); ++ tweet_inner_idx) {
                    String place = tweet_places.get(tweetidx).get(tweet_inner_idx);
                    Long createdat = created_at.get(tweetidx).get(tweet_inner_idx);

                    String geometry = "";
                    if (placesCache.containsKey(place))
                    {
                        geometry = placesCache.get(place);
                        System.out.print(" c");
                    }
                    else
                    {
                        // query it
                        query = QueryBuilder
                                .select(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_LOCATION.getColumnName())
                                .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_REFERRED_PLACE.getTableName())
                                .where(eq(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_CREATED_AT.getColumnName(), createdat))
                                .and(eq(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_POST_ID.getColumnName(), tweet))
                                .and(eq(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_PLACE_LITERAL.getColumnName(), place));

                        results = session.execute(query);
                        for (Row row : results)
                        {
                            String bbox = row.getString(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_REFERRED_PLACE.FLD_LOCATION.getColumnName());
                            placesCache.put(place, bbox);
                            geometry = bbox;
                        }
                        System.out.print(" q");
                    } // else query it

                    // insert


                    Statement upd = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                            .with(put(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), place, geometry))
                            .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), eventID));
                    try
                    {
                        session.execute(upd);
                    }
                    catch(InvalidQueryException ex)
                    {
                        System.err.print("query-failed:("+place+")");
                    }
                    System.out.print("[" + place+"]");

                } // for each place for that tweet
                System.out.println(" }");
            } // for each tweet

        } // for each event
    }

    /**
     * Method to efficiently populate events table with newly extracted location data. Input collections are essentialy
     * the data that location extraction fetched and processed. If an element of an event is not found, consider
     * increasing the time-window in the loc.extr. properties or reset the loc. extr. log, so as to fetch the omitted document.
     * @param mode
     * @param tweet_places_polygons
     * @param post_ids
     * @param article_places_polygons
     * @param permalinks
     */

    @Override
    public void updateEventsWithAllLocationPolygonPairs(OperationMode mode, ArrayList<Map<String,String>> tweet_places_polygons, ArrayList<Long> post_ids,ArrayList<Map<String,String>> article_places_polygons, ArrayList<String> permalinks)
    {
        System.out.println("Updating events' location data, mode :" + mode);
        // get all event ids
        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        ArrayList<String> eventIDs = new ArrayList<>();
        for(Row row : results)
        {
            eventIDs.add(row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));
        }

        switch(mode)
        {
            case ARTICLES:
                System.out.println("Updating events using " + permalinks.size() + " articles.");
                ArrayList<Set<String>> permalinksPerEvent = new ArrayList<>();
                // get all required data per event
                for(String eventID : eventIDs) {

                    Set<String> ev_permalinks = new HashSet<>();
                    // article source urls
                    query = QueryBuilder
                            .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName())
                            .from(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                            .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), eventID));
                    results = session.execute(query);
                    // for each article url
                    for (Row row : results)
                    {
                        Map<String, String> articleURLs = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), String.class, String.class);
                        ev_permalinks = articleURLs.keySet();
                    }
                    permalinksPerEvent.add(ev_permalinks);
                }

                for(int ev=0;ev < eventIDs.size(); ++ ev) {

                    String event_id = eventIDs.get(ev);
                    System.out.println("Inserting article location data into event " + ev + "/" + eventIDs.size() + ": [" + event_id + "]");
                    // add all permalinks
                    int permalinkCount = 1;
                    for (String perml : permalinksPerEvent.get(ev)) {
                        // find idx of perm
                        int idx = permalinks.indexOf(perml);
                        if (idx == -1) {
                            System.out.println("ERROR : could not find permalink " + perml);
                            continue;
                        }
                        // add all its geometries
                        if(article_places_polygons.get(idx).keySet().size() > 0)
                            System.out.print("\t\t>>> Inserting from url " + permalinkCount++ + "/" + permalinksPerEvent.get(ev).size() + " : "  + perml);
                        for (String locname : article_places_polygons.get(idx).keySet()) {
                            String geom = article_places_polygons.get(idx).get(locname);

                            System.out.println("  " + locname );


                            HashMap<String,String> tempmap = new HashMap<>();
                            tempmap.put(locname,geom);
                            Statement upd = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                                    .with(set(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), tempmap))
                                    .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),event_id));
                            try {
                                session.execute(upd);
                            }
                            catch(InvalidQueryException ex)
                            {
                                System.err.println("ERROR : update query failed for location: " +locname );

                            }


                        }
                    }
                }

                break;
            case TWEETS:
                System.out.println("Updating events using " + post_ids.size() + " tweets.");
                ArrayList<Set<Long>> tweetsPerEvent = new ArrayList<>();
                // get all required data per event
                for(String eventID : eventIDs) {
                    // twitter post IDs
                    Set<Long> ev_tweets = new HashSet<>();
                    query = QueryBuilder
                            .select(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName())
                            .from(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                            .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), eventID));
                    results = session.execute(query);
                    for (Row row : results)
                    {
                        Map<Long, String> tweet_ids = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(), Long.class, String.class);
                        ev_tweets = tweet_ids.keySet();
                    }
                    tweetsPerEvent.add(ev_tweets);
                }
                System.out.println("Tweets:");
                // add all tweets
                for(int ev=0;ev < eventIDs.size(); ++ ev)
                {
                    String event_id = eventIDs.get(ev);
                    System.out.println("Inserting tweet location data into event " + ev + "/" + eventIDs.size() + ": [" + event_id + "]");
                    int tweetCount=1;
                    for (long post_id : tweetsPerEvent.get(ev)) {
                        // find idx of perm
                        int idx = post_ids.indexOf(post_id);
                        if (idx == -1) {
                            System.err.println("ERROR : could not find tweet id " + post_id);
                            continue;
                        }
                        if(tweet_places_polygons.get(idx).keySet().size() > 0)
                        System.out.print("\t\t>>> Inserting from tweet " + tweetCount++ + "/" + tweetsPerEvent.get(ev).size() + " : "  + post_id);
                        // add all its geometries
                        for (String locname : tweet_places_polygons.get(idx).keySet()) {
                            String geom = tweet_places_polygons.get(idx).get(locname);
                            System.out.println("  " + locname );

                            HashMap<String,String> tempmap = new HashMap<>();
                            tempmap.put(locname,geom);
                            Statement upd = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                                    .with(set(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), tempmap))
                                    .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),event_id));
                            try
                            {
                                session.execute(upd);
                            }
                            catch(InvalidQueryException ex)
                            {
                                System.err.println("ERROR : update query failed for location: " +locname );

                            }
                        }
                    }
                }
                break;

        }

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



    /**
     * Function to send events to strabon for storage and/or change detection
     */
    @Override
    public void storeAndChangeDetectionEvents(String strabonURL)
    {
        System.out.println("Sending events to Strabon.");
        // get all events, fields: id, title, date, placemappings
        // store each field of interest in an arraylist

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
            // get the id
            String id = (row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));

            // place-mappings: If null, skip the processing
            Map<String,String> locpoly = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(),String.class,String.class);
            if (locpoly.isEmpty())
            {
                System.out.println("Skipping processing event " + count++ + ":" + id + " due to no assigned geometries." );
                continue;
            }

            // get the title
            String title = (row.getString(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName()));
            // get date
            String date = (row.getString(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName()));

            // reconstruct the entries in the format expected by strabon
            String payload="";
            try
            {
                payload = GeometryFormatTransformer.EventRowToStrabonJSON(id,title,date,locpoly);

            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            //System.out.println("payload is:<" + payload + ">"); // debugprint
            if(payload.isEmpty())
            {
                System.out.println("Empty payload, won't send anything.");
                return;
            }

            // send http request
            // TODO: make new connection for each send or make connection out of loop?
            // TODO: put target url in the properties file on the module this function will
            // end up in
            String URLstr = strabonURL;
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
                System.out.println("Sending event + [" + id + "].");
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
                System.out.println("server response:\n\t" + resp);

                if (resp.equals("{\"code\":400,\"message\":\"exception\"}"))
                {
                    System.err.println("Server request failed.");
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
    @Override
    public void doHotfix()
    {
        Collection<Map<String, Object>> items = loadAllArticlesHotfix(-1);
        for (Map<String, Object> article : items)
        {

            long pub_date = (long) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
            String reversed_host = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName());
            String entryurl = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName());
            String clean_text = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CLEAN_TEXT.getColumnName());
            Long crawl_id = (Long) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWL_ID.getColumnName());
            String feed_url = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_FEED_URL.getColumnName());
            Set<String> place_literal = (Set<String>)article.get(Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName());
            String raw_text = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_RAW_TEXT.getColumnName());
            Long crawled = (Long) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName());
            String title = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_TITLE.getColumnName());
            String lang = (String) article.get(Cassandra.RSS.TBL_ARTICLES.FLD_LANGUAGE.getColumnName());

            String year_month_day = Utils.extractYearMonthDayLiteral(pub_date);


            // populate the rest of the tables. (from base cassandra repo)

            // insert in articles_per_published_date
            Statement insert = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), "news_articles_per_published_date")
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), pub_date)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), entryurl)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName(), feed_url)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.getColumnName(), raw_text)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(), clean_text)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(), crawled)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWL_ID.getColumnName(), crawl_id)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.getColumnName(), lang)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), place_literal)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(), title);
            session.execute(insert);
            // insert in articles_per_crawled_date
            insert = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), "news_articles_per_crawled_date")
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(), crawled)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), pub_date)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), entryurl)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName(),feed_url)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.getColumnName(),raw_text)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(), clean_text)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.getColumnName(), lang)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), place_literal)
                    .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(), title);
            session.execute(insert);


        }

        Collection<Map<String, Object>> tweets = loadAllTweetsHotfix(-1);

        for (Map<String, Object> eachItem : tweets) {
            long post_id = (long) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName());
            String tweet_identified_lang = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName());
            String account_name = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_ACCOUNT_NAME.getColumnName());
            String coord = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_COORDINATES.getColumnName());
            long timestamp_created = (long) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName());
            String externalLinks = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_EXTERNAL_LINKS.getColumnName());
            String followers = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_FOLLOWERS_WHEN_PUBLISHED.getColumnName());
            String place = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PLACE.getColumnName());
            String retweet = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_RETWEET_CNT.getColumnName());
            String tweet = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_TWEET.getColumnName());
            String permalink = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PERMALINK.getColumnName());

            String year_month_day = Utils.extractYearMonthDayLiteral(timestamp_created);

            Statement insert_created_at
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_DATE.getTableName())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName(), timestamp_created)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName(), post_id)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName(), tweet)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.getColumnName(), permalink);
            session.execute(insert_created_at);

//            Statement insert_place
//                    = QueryBuilder
//                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_PLACE.getTableName())
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), "")
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_CREATED_AT.getColumnName(), timestamp_created)
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_POST_ID.getColumnName(), post_id)
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_ACCOUNT_NAME.getColumnName(), account_name)
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_PLACE.getColumnName(), plCodec.serialize(tplace, ProtocolVersion.V2))
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_TWEET.getColumnName(), tweet)
//                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_PERMALINK.getColumnName(), permalink);
//            session.execute(insert_place);


        }



    }



    public Collection<Map<String, Object>> loadAllTweetsHotfix(int atmost) {
        System.out.println("loading tweets...");

        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        if (atmost > 0)
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POST.getTableName())
                    .limit(atmost);
        else
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POST.getTableName())
                    ;

        results = session.execute(select);
        for (Row row : results) {
            Map<String, Object> res = new HashMap();

            long created_at = row.getLong(Cassandra.Twitter.TBL_TWITTER_POST.FLD_CREATED_AT.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_POST.FLD_CREATED_AT.getColumnName(), created_at);
            long post_id = row.getLong(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName(), post_id);
            String account_name = row.getString(Cassandra.Twitter.TBL_TWITTER_POST.FLD_ACCOUNT_NAME.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_POST.FLD_ACCOUNT_NAME.getColumnName(), account_name);
            String lang = row.getString(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName(), lang);
            String tweet = row.getString(Cassandra.Twitter.TBL_TWITTER_POST.FLD_TWEET.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_POST.FLD_TWEET.getColumnName(), tweet);
            String permalink = row.getString(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PERMALINK.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PERMALINK.getColumnName(), permalink);
            // append
            out.add(res);
        }
        System.out.println(String.format("loaded %d tweets", out.size()));
        return Collections.unmodifiableCollection(out);
    }


    public Collection<Map<String, Object>> loadAllArticlesHotfix(int atmost) {
        System.out.println("loading  ~ALL~ articles...");
        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        if (atmost > 0)
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES.getTableName())
                    .limit(atmost)
                    ;
        else
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES.getTableName())
                    ;
        results = session.execute(select);
        for (Row row : results) {
            Map<String, Object> res = new HashMap();
            long published = row.getLong(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName(), published);
            String entry_url = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), entry_url);
            Set<String> place_literal = row.getSet(Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName(), String.class);
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName(), place_literal);
            String feed_url = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_FEED_URL.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_FEED_URL.getColumnName(), feed_url);
            long crawl_id = row.getLong(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWL_ID.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWL_ID.getColumnName(), crawl_id);
            String raw_text = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_RAW_TEXT.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_RAW_TEXT.getColumnName(), raw_text);
            String clean_text = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_CLEAN_TEXT.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_CLEAN_TEXT.getColumnName(), clean_text);
            long crawled = row.getLong(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName(), crawled);
            String lang = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_LANGUAGE.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_LANGUAGE.getColumnName(), lang);
            String title = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_TITLE.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES.FLD_TITLE.getColumnName(), title);
            // append
            //assert (published >= from) : String.format("query totally wrong: published=%d < from=%d", published, from);
            out.add(res);
        }
        // debug
        System.out.println(String.format("loaded %d articles", out.size()));
        // debug
        return Collections.unmodifiableCollection(out);
    }

}
