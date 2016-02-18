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

import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import com.datastax.driver.mapping.MappingManager;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import gr.demokritos.iit.crawlers.twitter.structures.TGeoLoc;
import gr.demokritos.iit.crawlers.twitter.structures.TPlace;
import gr.demokritos.iit.crawlers.twitter.url.IURLUnshortener;
import gr.demokritos.iit.base.util.langdetect.CybozuLangDetect;
import gr.demokritos.iit.base.util.Utils;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.User;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraRepository extends AbstractRepository implements IRepository {

    /**
     * an active Cassandra session
     */
    private final Session session;

    private final MappingManager mapper;
    /**
     * serialize and deserialize {@link TGeoLoc} UDTs
     */
    final TypeCodec<TGeoLoc> geoCodec;
    /**
     * serialize and deserialize {@link TPlace} UDTs
     */
    final TypeCodec<TPlace> plCodec;

    /**
     *
     * @param session
     * @param unshortenerArg
     */
    public CassandraRepository(Session session, IURLUnshortener unshortenerArg) {
        super(unshortenerArg);
        this.session = session;
        this.mapper = new MappingManager(session);
        this.geoCodec = mapper.udtCodec(TGeoLoc.class);
        this.plCodec = mapper.udtCodec(TPlace.class);
    }

    /**
     * default constructor
     *
     * @param session
     */
    public CassandraRepository(Session session) {
        super();
        this.session = session;
        this.mapper = new MappingManager(session);
        this.geoCodec = mapper.udtCodec(TGeoLoc.class);
        this.plCodec = mapper.udtCodec(TPlace.class);
    }

    @Override
    public Collection<SourceAccount> getAccounts() {
        Statement select = QueryBuilder.select().all().from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_SOURCE.getTableName());
        ResultSet results;
        results = session.execute(select);

        Collection<SourceAccount> res = new LinkedHashSet();

        for (Row row : results) {
            String screen_name = row.getString(Cassandra.Twitter.TBL_TWITTER_SOURCE.FLD_ACCOUNT_NAME.getColumnName());
            Boolean active = row.getBool(Cassandra.Twitter.TBL_TWITTER_SOURCE.FLD_ACTIVE.getColumnName());
            res.add(new SourceAccount(screen_name, active));
        }
        return res;
    }

    @Override
    public void saveAccount(String accountName) {
        saveAccount(accountName, Boolean.TRUE);
    }

    @Override
    public long insertUser(User user) {
        Statement insert
                = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_USER.getTableName())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_USER_ID.getColumnName(), user.getId())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_FOLLOWERS_CNT.getColumnName(), user.getFollowersCount())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_FRIENDS_CNT.getColumnName(), user.getFriendsCount())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_LISTED_CNT.getColumnName(), user.getListedCount())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_LOCATION.getColumnName(), user.getLocation())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_NAME.getColumnName(), user.getName())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_ACCOUNT_NAME.getColumnName(), user.getScreenName())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_STATUSES_CNT.getColumnName(), user.getStatusesCount())
                .value(Cassandra.Twitter.TBL_TWITTER_USER.FLD_TIMEZONE.getColumnName(), user.getTimeZone());
        session.execute(insert);
        return user.getId();
    }

    @Override
    public void updateUser(User user) {
        insertUser(user);
    }

    @Override
    public boolean existsUser(long userID) {
        String key = Cassandra.Twitter.TBL_TWITTER_USER.FLD_ACCOUNT_NAME.getColumnName();
        Statement select = QueryBuilder
                .select(key)
                .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_USER.getTableName())
                .where(eq(Cassandra.Twitter.TBL_TWITTER_USER.FLD_USER_ID.getColumnName(), userID));
        ResultSet results = session.execute(select);

        Row one = results.one();
        if (one != null) {
            String sn = one.getString(key);
            if (sn != null && !sn.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void insertPost(Status post, long api_user_id, String account_name, int followersWhenPublished, CrawlEngine engine, long engine_id) {
        if (post == null) {
            return;
        }
        String tweet = post.getText();
        if (tweet == null || tweet.trim().isEmpty()) {
            return;
        }
        tweet = tweet.trim();
        String tweet_identified_lang = post.getLang();
        // identify tweet language, if needed
        if (tweet_identified_lang == null || tweet_identified_lang.equalsIgnoreCase(TWEET_UNDEFINED_LANG)) {
            tweet_identified_lang = CybozuLangDetect.getInstance().identifyLanguage(cleanTweetFromURLs(post), TWEET_UNDEFINED_LANG);
        }
        // get post ID
        Long post_id = post.getId();

        String coordinates = "";
        GeoLocation geoLocation = post.getGeoLocation();
        // create the UDT for geolocation 
        TGeoLoc geoloc;
        if (geoLocation != null) {
            coordinates = post.getGeoLocation().toString();
            geoloc = new TGeoLoc(post.getGeoLocation().getLatitude(), post.getGeoLocation().getLongitude());
        } else {
            geoloc = new TGeoLoc(0.0, 0.0);
        }
        // generate the geolocation bucket to utilize later
        String geo_bucket_literal = extractGeolocationLiteral(geoLocation);
        Place place = post.getPlace();
        TPlace tplace;
        String sPlace = "";
        if (place != null) {
            String sFullName = place.getFullName();
            String sCountry = place.getCountry();
            if (sFullName != null) {
                sPlace = sFullName;
            } else if (sCountry != null) {
                sPlace = sCountry;
            }
            tplace = new TPlace(place);
        } else {
            tplace = new TPlace();
        }

        long timestamp_created = post.getCreatedAt().getTime();

        // extract tweet permalink
        String permalink = extractTweetPermalink(account_name, post_id);

        // get URL links, if there 
        List<String> external_links = getURLEntities(post);
        // unshorten URLs
        external_links = unshortenURLs(external_links);

        // insert post in twitter_post
        Statement insert
                = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POST.getTableName())
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName(), post_id)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_COORDINATES.getColumnName(), coordinates)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_CREATED_AT.getColumnName(), timestamp_created)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_EXTERNAL_LINKS.getColumnName(), external_links)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_FOLLOWERS_WHEN_PUBLISHED.getColumnName(), followersWhenPublished)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PLACE.getColumnName(), sPlace)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_RETWEET_CNT.getColumnName(), post.getRetweetCount())
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_TWEET.getColumnName(), tweet)
                .value(Cassandra.Twitter.TBL_TWITTER_POST.FLD_PERMALINK.getColumnName(), permalink);
        session.execute(insert);

        // insert metadata in twitter_hashtags_per_post
        // extract hashtags
        HashtagEntity[] hashtagEntities = post.getHashtagEntities();

        for (HashtagEntity hashtag : hashtagEntities) {
            Statement insert_hashtag
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_HASHTAG.getTableName())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_HASHTAG.getColumnName(), hashtag.getText())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_CREATED_AT.getColumnName(), timestamp_created)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_POST_ID.getColumnName(), post_id)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_TWEET.getColumnName(), tweet)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_HASHTAG.FLD_PERMALINK.getColumnName(), permalink);
            session.execute(insert_hashtag);
        }

        // insert metadata in twitter_external_urls_per_post
        for (String external_url : external_links) {
            Statement insert_external_url
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_EXTERNAL_URL.getTableName())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_EXTERNAL_URL.getColumnName(), external_url)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_CREATED_AT.getColumnName(), timestamp_created)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_POST_ID.getColumnName(), post_id)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_TWEET.getColumnName(), tweet)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_PERMALINK.getColumnName(), permalink);
            session.execute(insert_external_url);
        }
        // insert metadata in twitter_created_at_per_post
        // extract year_month_day data to divide to buckets.
        String year_month_day = Utils.extractYearMonthDayLiteral(post.getCreatedAt());

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

        if (!geo_bucket_literal.isEmpty()) {
            System.out.println("inserting: " + geoloc.toString());
            // insert metadata at twitter_coordinates_per_post
            Statement insert_coords
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_COORDINATES.getTableName())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_GEO_BUCKET.getColumnName(), geo_bucket_literal)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_CREATED_AT.getColumnName(), timestamp_created)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_GEOLOCATION.getColumnName(), geoCodec.serialize(geoloc, ProtocolVersion.V2))
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_POST_ID.getColumnName(), post_id)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_TWEET.getColumnName(), tweet)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_COORDINATE.FLD_PERMALINK.getColumnName(), permalink);
            session.execute(insert_coords);
        }
        if (!sPlace.isEmpty()) {
            System.out.println("inserting: " + tplace.toString() + ", for post: " + post.getId());
            // insert metadata at twitter_place_per_post
            Statement insert_place
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_PLACE.getTableName())
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), sPlace)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_CREATED_AT.getColumnName(), timestamp_created)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_POST_ID.getColumnName(), post_id)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_LANGUAGE.getColumnName(), tweet_identified_lang)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_PLACE.getColumnName(), plCodec.serialize(tplace, ProtocolVersion.V2))
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_TWEET.getColumnName(), tweet)
                    .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_PLACE.FLD_PERMALINK.getColumnName(), permalink);
            session.execute(insert_place);
        }
        // insert metadata at twitter_engine_per_post
        Statement insert_engine
                = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_ENGINE.getTableName())
                .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_ENGINE.FLD_ENGINE_TYPE.getColumnName(), engine.toString().toLowerCase())
                .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_ENGINE.FLD_ENGINE_ID.getColumnName(), engine_id)
                .value(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_ENGINE.FLD_POST_ID.getColumnName(), post_id);
        session.execute(insert_engine);
    }

    @Override
    public void updatePost(Status post) {
        String tweet_identified_lang = post.getLang();
        if (tweet_identified_lang == null || tweet_identified_lang.equalsIgnoreCase(TWEET_UNDEFINED_LANG)) {
            tweet_identified_lang = CybozuLangDetect.getInstance().identifyLanguage(cleanTweetFromURLs(post), TWEET_UNDEFINED_LANG);
        }
        Statement update = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POST.getTableName())
                .with(QueryBuilder.set(Cassandra.Twitter.TBL_TWITTER_POST.FLD_RETWEET_CNT.getColumnName(), post.getRetweetCount()))
                .where(QueryBuilder.eq(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName(), post.getId()))
                .and(QueryBuilder.eq(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName(), tweet_identified_lang));
        session.execute(update);
    }

    @Override
    public boolean existsPost(long post_id) {
        Statement select
                = QueryBuilder
                .select()
                .column(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName())
                .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POST.getTableName())
                .where(eq(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName(), post_id)).limit(1);
        ResultSet result = session.execute(select);

        if (result != null) {
            Row one = result.one();
            if (one != null) {
                String lang = one.getString(Cassandra.Twitter.TBL_TWITTER_POST.FLD_LANGUAGE.getColumnName());
                return (lang != null && !lang.isEmpty());
            }
        }
        return false;
    }

    @Override
    public long scheduleInitialized(CrawlEngine engine_type) {
        String key = Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENGINE_ID.getColumnName();
        Statement select = QueryBuilder
                .select(key)
                .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_LOG.getTableName())
                .where(eq(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENGINE_TYPE.getColumnName(), engine_type.toString().toLowerCase())).limit(1);
        ResultSet results = session.execute(select);
        Row one = results.one();

        long max_existing = 0l;

        if (one != null) {
            max_existing = one.getLong(key);
        }
        long current = max_existing + 1;
        Statement insert = QueryBuilder.insertInto(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_LOG.getTableName())
                .value(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENGINE_TYPE.getColumnName(), engine_type.toString())
                .value(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENGINE_ID.getColumnName(), current)
                .value(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_STARTED.getColumnName(), new Date().getTime());
        session.execute(insert);
        return current;
    }

    @Override
    public void scheduleFinalized(long schedule_id, CrawlEngine engine_type) {
        Statement update = QueryBuilder.update(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_LOG.getTableName())
                .with(QueryBuilder.set(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENDED.getColumnName(), new Date().getTime()))
                .where(QueryBuilder.eq(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENGINE_TYPE.getColumnName(), engine_type.toString().toLowerCase()))
                .and(QueryBuilder.eq(Cassandra.Twitter.TBL_TWITTER_LOG.FLD_ENGINE_ID.getColumnName(), schedule_id));
        session.execute(update);
    }

    @Override
    public boolean existSource(String account_name) {
        Statement select
                = QueryBuilder
                .select()
                .countAll()
                .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_SOURCE.getTableName())
                .where(eq(Cassandra.Twitter.TBL_TWITTER_SOURCE.FLD_ACCOUNT_NAME.getColumnName(), account_name)).limit(1);
        ResultSet result = session.execute(select);

        if (result != null) {
            Row one = result.one();
            if (one != null) {
                long cnt = one.getLong(0);
                return cnt > 0;
            }
        }
        return false;
    }

    @Override
    public void saveAccount(String account_name, boolean active) {
        Statement insert
                = QueryBuilder
                .insertInto(Cassandra.Twitter.Tables.TWITTER_SOURCE.getTableName())
                .value(Cassandra.Twitter.TBL_TWITTER_SOURCE.FLD_ACCOUNT_NAME.getColumnName(), account_name)
                .value(Cassandra.Twitter.TBL_TWITTER_SOURCE.FLD_ACTIVE.getColumnName(), active);
        session.execute(insert);
    }

    @Override
    public Map<String, Object> getUserInfo(String account_name) {
        Statement select
                = QueryBuilder
                .select()
                .all()
                .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_USER.getTableName())
                .where(eq(Cassandra.Twitter.TBL_TWITTER_USER.FLD_ACCOUNT_NAME.getColumnName(), account_name));
        ResultSet results;
        results = session.execute(select);

        Map<String, Object> res = new HashMap();
        for (Row row : results) {
            long user_id = row.getLong(Cassandra.Twitter.TBL_TWITTER_USER.FLD_USER_ID.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_USER_ID.getColumnName(), user_id);
            long followers_count = row.getLong(Cassandra.Twitter.TBL_TWITTER_USER.FLD_FOLLOWERS_CNT.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_FOLLOWERS_CNT.getColumnName(), followers_count);
            long friends_count = row.getLong(Cassandra.Twitter.TBL_TWITTER_USER.FLD_FRIENDS_CNT.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_FRIENDS_CNT.getColumnName(), friends_count);
            long listed_count = row.getLong(Cassandra.Twitter.TBL_TWITTER_USER.FLD_LISTED_CNT.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_LISTED_CNT.getColumnName(), listed_count);
            String name = row.getString(Cassandra.Twitter.TBL_TWITTER_USER.FLD_NAME.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_NAME.getColumnName(), name);
            String location = row.getString(Cassandra.Twitter.TBL_TWITTER_USER.FLD_LOCATION.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_LOCATION.getColumnName(), location);
            long statuses_count = row.getLong(Cassandra.Twitter.TBL_TWITTER_USER.FLD_STATUSES_CNT.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_STATUSES_CNT.getColumnName(), statuses_count);
            String timezone = row.getString(Cassandra.Twitter.TBL_TWITTER_USER.FLD_TIMEZONE.getColumnName());
            res.put(Cassandra.Twitter.TBL_TWITTER_USER.FLD_TIMEZONE.getColumnName(), timezone);
        }
        return Collections.unmodifiableMap(res);
    }

    @Override
    public LinkedHashMap<Integer, String> getUsersSortedByMaxRetweets() {
        throw new UnsupportedOperationException("Not supported.");
    }
}
