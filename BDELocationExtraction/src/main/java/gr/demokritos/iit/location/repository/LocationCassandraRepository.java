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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.structs.LocSched;

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
        System.out.println(String.format("updating %s with places: %s", permalink, places_polygons.keySet().toString()));
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
    }

    @Override
    public void updateTweetsWithReferredPlaceMetadata(long post_id, Map<String, String> places_polygons) {
        if (places_polygons == null || places_polygons.isEmpty()) {
            return;
        }
        System.out.println("updating: " + post_id + ", with: " + places_polygons.keySet().toString());
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
}
