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
        Statement select = QueryBuilder.select().all().from(session.getLoggedKeyspace(), Table.TWITTER_SOURCE.table_name);
        ResultSet results;
        results = session.execute(select);

        Collection<SourceAccount> res = new LinkedHashSet();

        for (Row row : results) {
            String screen_name = row.getString(TBL_TWITTER_SOURCE.FLD_ACCOUNT_NAME.column);
            Boolean active = row.getBool(TBL_TWITTER_SOURCE.FLD_ACTIVE.column);
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
                .insertInto(session.getLoggedKeyspace(), Table.TWITTER_USER.table_name)
                .value(TBL_TWITTER_USER.FLD_USER_ID.column, user.getId())
                .value(TBL_TWITTER_USER.FLD_FOLLOWERS_CNT.column, user.getFollowersCount())
                .value(TBL_TWITTER_USER.FLD_FRIENDS_CNT.column, user.getFriendsCount())
                .value(TBL_TWITTER_USER.FLD_LISTED_CNT.column, user.getListedCount())
                .value(TBL_TWITTER_USER.FLD_LOCATION.column, user.getLocation())
                .value(TBL_TWITTER_USER.FLD_NAME.column, user.getName())
                .value(TBL_TWITTER_USER.FLD_ACCOUNT_NAME.column, user.getScreenName())
                .value(TBL_TWITTER_USER.FLD_STATUSES_CNT.column, user.getStatusesCount())
                .value(TBL_TWITTER_USER.FLD_TIMEZONE.column, user.getTimeZone());
        session.execute(insert);
        return user.getId();
    }

    @Override
    public void updateUser(User user) {
        insertUser(user);
    }

    @Override
    public boolean existsUser(long userID) {
        String key = TBL_TWITTER_USER.FLD_ACCOUNT_NAME.column;
        Statement select = QueryBuilder
                .select(key)
                .from(session.getLoggedKeyspace(), Table.TWITTER_USER.table_name)
                .where(eq(TBL_TWITTER_USER.FLD_USER_ID.column, userID));
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
        String permalink = "https://twitter.com/" + account_name + "/status/" + post_id;

        // get URL links, if there 
        List<String> external_links = getURLEntities(post);
        // unshorten URLs
        external_links = unshortenURLs(external_links);

        // insert post in twitter_post
        Statement insert
                = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POST.table_name)
                .value(TBL_TWITTER_POST.FLD_POST_ID.column, post_id)
                .value(TBL_TWITTER_POST.FLD_LANGUAGE.column, tweet_identified_lang)
                .value(TBL_TWITTER_POST.FLD_ACCOUNT_NAME.column, account_name)
                .value(TBL_TWITTER_POST.FLD_COORDINATES.column, coordinates)
                .value(TBL_TWITTER_POST.FLD_CREATED_AT.column, timestamp_created)
                .value(TBL_TWITTER_POST.FLD_EXTERNAL_LINKS.column, external_links)
                .value(TBL_TWITTER_POST.FLD_FOLLOWERS_WHEN_PUBLISHED.column, followersWhenPublished)
                .value(TBL_TWITTER_POST.FLD_PLACE.column, sPlace)
                .value(TBL_TWITTER_POST.FLD_RETWEET_CNT.column, post.getRetweetCount())
                .value(TBL_TWITTER_POST.FLD_TWEET.column, tweet)
                .value(TBL_TWITTER_POST.FLD_PERMALINK.column, permalink);
        session.execute(insert);

        // insert metadata in twitter_hashtags_per_post
        // extract hashtags
        HashtagEntity[] hashtagEntities = post.getHashtagEntities();

        for (HashtagEntity hashtag : hashtagEntities) {
            Statement insert_hashtag
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POSTS_PER_HASHTAG.table_name)
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_HASHTAG.column, hashtag.getText())
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_CREATED_AT.column, timestamp_created)
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_POST_ID.column, post_id)
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_ACCOUNT_NAME.column, account_name)
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_LANGUAGE.column, tweet_identified_lang)
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_TWEET.column, tweet)
                    .value(TBL_TWITTER_POSTS_PER_HASHTAG.FLD_PERMALINK.column, permalink);
            session.execute(insert_hashtag);
        }

        // insert metadata in twitter_external_urls_per_post
        for (String external_url : external_links) {
            Statement insert_external_url
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POSTS_PER_EXTERNAL_URL.table_name)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_EXTERNAL_URL.column, external_url)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_CREATED_AT.column, timestamp_created)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_POST_ID.column, post_id)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_ACCOUNT_NAME.column, account_name)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_LANGUAGE.column, tweet_identified_lang)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_TWEET.column, tweet)
                    .value(TBL_TWITTER_POSTS_PER_EXTERNAL_URL.FLD_PERMALINK.column, permalink);
            session.execute(insert_external_url);
        }
        // insert metadata in twitter_created_at_per_post
        // extract year_month_day data to divide to buckets.
        String year_month_day = Utils.extractYearMonthDayLiteral(post.getCreatedAt());

        Statement insert_created_at
                = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POSTS_PER_DATE.table_name)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.column, year_month_day)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.column, timestamp_created)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.column, post_id)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.column, account_name)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.column, tweet_identified_lang)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.column, tweet)
                .value(TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.column, permalink);
        session.execute(insert_created_at);

        if (!geo_bucket_literal.isEmpty()) {
            System.out.println("inserting: " + geoloc.toString());
            // insert metadata at twitter_coordinates_per_post
            Statement insert_coords
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POSTS_PER_COORDINATES.table_name)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_GEO_BUCKET.column, geo_bucket_literal)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_CREATED_AT.column, timestamp_created)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_GEOLOCATION.column, geoCodec.serialize(geoloc, ProtocolVersion.V2))
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_POST_ID.column, post_id)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_ACCOUNT_NAME.column, account_name)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_LANGUAGE.column, tweet_identified_lang)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_TWEET.column, tweet)
                    .value(TBL_TWITTER_POSTS_PER_COORDINATE.FLD_PERMALINK.column, permalink);
            session.execute(insert_coords);
        }
        if (!sPlace.isEmpty()) {
            System.out.println("inserting: " + tplace.toString() + ", for post: " + post.getId());
            // insert metadata at twitter_place_per_post
            Statement insert_place
                    = QueryBuilder
                    .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POSTS_PER_PLACE.table_name)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_PLACE_LITERAL.column, sPlace)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_CREATED_AT.column, timestamp_created)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_POST_ID.column, post_id)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_ACCOUNT_NAME.column, account_name)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_LANGUAGE.column, tweet_identified_lang)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_PLACE.column, plCodec.serialize(tplace, ProtocolVersion.V2))
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_TWEET.column, tweet)
                    .value(TBL_TWITTER_POSTS_PER_PLACE.FLD_PERMALINK.column, permalink);
            session.execute(insert_place);
        }
        // insert metadata at twitter_engine_per_post
        Statement insert_engine
                = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Table.TWITTER_POSTS_PER_ENGINE.table_name)
                .value(TBL_TWITTER_POSTS_PER_ENGINE.FLD_ENGINE_TYPE.column, engine.toString().toLowerCase())
                .value(TBL_TWITTER_POSTS_PER_ENGINE.FLD_ENGINE_ID.column, engine_id)
                .value(TBL_TWITTER_POSTS_PER_ENGINE.FLD_POST_ID.column, post_id);
        session.execute(insert_engine);
    }

    @Override
    public void updatePost(Status post) {
        String tweet_identified_lang = post.getLang();
        if (tweet_identified_lang == null || tweet_identified_lang.equalsIgnoreCase(TWEET_UNDEFINED_LANG)) {
            tweet_identified_lang = CybozuLangDetect.getInstance().identifyLanguage(cleanTweetFromURLs(post), TWEET_UNDEFINED_LANG);
        }
        Statement update = QueryBuilder.update(session.getLoggedKeyspace(), Table.TWITTER_POST.table_name)
                .with(QueryBuilder.set(TBL_TWITTER_POST.FLD_RETWEET_CNT.column, post.getRetweetCount()))
                .where(QueryBuilder.eq(TBL_TWITTER_POST.FLD_POST_ID.column, post.getId()))
                .and(QueryBuilder.eq(TBL_TWITTER_POST.FLD_LANGUAGE.column, tweet_identified_lang));
        session.execute(update);
    }

    @Override
    public boolean existsPost(long post_id) {
        Statement select
                = QueryBuilder
                .select()
                .column(TBL_TWITTER_POST.FLD_LANGUAGE.column)
                .from(session.getLoggedKeyspace(), Table.TWITTER_POST.table_name)
                .where(eq(TBL_TWITTER_POST.FLD_POST_ID.column, post_id)).limit(1);
        ResultSet result = session.execute(select);

        if (result != null) {
            Row one = result.one();
            if (one != null) {
                String lang = one.getString(TBL_TWITTER_POST.FLD_LANGUAGE.column);
                return (lang != null && !lang.isEmpty());
            }
        }
        return false;
    }

    @Override
    public long scheduleInitialized(CrawlEngine engine_type) {
        String key = TBL_TWITTER_LOG.FLD_ENGINE_ID.column;
        Statement select = QueryBuilder
                .select(key)
                .from(session.getLoggedKeyspace(), Table.TWITTER_LOG.table_name)
                .where(eq(TBL_TWITTER_LOG.FLD_ENGINE_TYPE.column, engine_type.toString().toLowerCase())).limit(1);
        ResultSet results = session.execute(select);
        Row one = results.one();

        long max_existing = 0l;

        if (one != null) {
            max_existing = one.getLong(key);
        }
        long current = max_existing + 1;
        Statement insert = QueryBuilder.insertInto(session.getLoggedKeyspace(), Table.TWITTER_LOG.table_name)
                .value(TBL_TWITTER_LOG.FLD_ENGINE_TYPE.column, engine_type.toString())
                .value(TBL_TWITTER_LOG.FLD_ENGINE_ID.column, current)
                .value(TBL_TWITTER_LOG.FLD_STARTED.column, new Date().getTime());
        session.execute(insert);
        return current;
    }

    @Override
    public void scheduleFinalized(long schedule_id, CrawlEngine engine_type) {
        Statement update = QueryBuilder.update(session.getLoggedKeyspace(), Table.TWITTER_LOG.table_name)
                .with(QueryBuilder.set(TBL_TWITTER_LOG.FLD_ENDED.column, new Date().getTime()))
                .where(QueryBuilder.eq(TBL_TWITTER_LOG.FLD_ENGINE_TYPE.column, engine_type.toString().toLowerCase()))
                .and(QueryBuilder.eq(TBL_TWITTER_LOG.FLD_ENGINE_ID.column, schedule_id));
        session.execute(update);
    }

    @Override
    public boolean existSource(String account_name) {
        Statement select
                = QueryBuilder
                .select()
                .countAll()
                .from(session.getLoggedKeyspace(), Table.TWITTER_SOURCE.table_name)
                .where(eq(TBL_TWITTER_SOURCE.FLD_ACCOUNT_NAME.column, account_name)).limit(1);
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
                .insertInto(Table.TWITTER_SOURCE.table_name)
                .value(TBL_TWITTER_SOURCE.FLD_ACCOUNT_NAME.column, account_name)
                .value(TBL_TWITTER_SOURCE.FLD_ACTIVE.column, active);
        session.execute(insert);
    }

    @Override
    public Map<String, Object> getUserInfo(String account_name) {
        Statement select
                = QueryBuilder
                .select()
                .all()
                .from(session.getLoggedKeyspace(), Table.TWITTER_USER.table_name)
                .where(eq(TBL_TWITTER_USER.FLD_ACCOUNT_NAME.column, account_name));
        ResultSet results;
        results = session.execute(select);

        Map<String, Object> res = new HashMap();
        for (Row row : results) {
            long user_id = row.getLong(TBL_TWITTER_USER.FLD_USER_ID.column);
            res.put(TBL_TWITTER_USER.FLD_USER_ID.column, user_id);
            long followers_count = row.getLong(TBL_TWITTER_USER.FLD_FOLLOWERS_CNT.column);
            res.put(TBL_TWITTER_USER.FLD_FOLLOWERS_CNT.column, followers_count);
            long friends_count = row.getLong(TBL_TWITTER_USER.FLD_FRIENDS_CNT.column);
            res.put(TBL_TWITTER_USER.FLD_FRIENDS_CNT.column, friends_count);
            long listed_count = row.getLong(TBL_TWITTER_USER.FLD_LISTED_CNT.column);
            res.put(TBL_TWITTER_USER.FLD_LISTED_CNT.column, listed_count);
            String name = row.getString(TBL_TWITTER_USER.FLD_NAME.column);
            res.put(TBL_TWITTER_USER.FLD_NAME.column, name);
            String location = row.getString(TBL_TWITTER_USER.FLD_LOCATION.column);
            res.put(TBL_TWITTER_USER.FLD_LOCATION.column, location);
            long statuses_count = row.getLong(TBL_TWITTER_USER.FLD_STATUSES_CNT.column);
            res.put(TBL_TWITTER_USER.FLD_STATUSES_CNT.column, statuses_count);
            String timezone = row.getString(TBL_TWITTER_USER.FLD_TIMEZONE.column);
            res.put(TBL_TWITTER_USER.FLD_TIMEZONE.column, timezone);
        }
        return Collections.unmodifiableMap(res);
    }

    @Override
    public LinkedHashMap<Integer, String> getUsersSortedByMaxRetweets() {
        throw new UnsupportedOperationException("Not supported.");
    }

    // cassandra tables
    enum Table {

        TWITTER_SOURCE("twitter_source"),
        TWITTER_USER("twitter_user"),
        TWITTER_POST("twitter_post"),
        TWITTER_POSTS_PER_DATE("twitter_posts_per_date"),
        TWITTER_POSTS_PER_HASHTAG("twitter_posts_per_hashtag"),
        TWITTER_POSTS_PER_EXTERNAL_URL("twitter_posts_per_external_url"),
        TWITTER_POSTS_PER_COORDINATES("twitter_posts_per_coordinates"),
        TWITTER_POSTS_PER_PLACE("twitter_posts_per_place"),
        TWITTER_POSTS_PER_ENGINE("twitter_posts_per_engine"),
        TWITTER_LOG("twitter_log");
        private final String table_name;

        private Table(String table_name) {
            this.table_name = table_name;
        }
    }

    // cassandra table fields
    enum TBL_TWITTER_SOURCE {

        FLD_ACCOUNT_NAME("account_name"),
        FLD_ACTIVE("active");
        private final String column;

        private TBL_TWITTER_SOURCE(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_USER {

        FLD_USER_ID("user_id"),
        FLD_FOLLOWERS_CNT("followers_count"),
        FLD_FRIENDS_CNT("friends_count"),
        FLD_LISTED_CNT("listed_count"),
        FLD_NAME("name"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LOCATION("location"),
        FLD_STATUSES_CNT("statuses_count"),
        FLD_TIMEZONE("timezone");
        private final String column;

        private TBL_TWITTER_USER(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POST {

        FLD_POST_ID("post_id"),
        FLD_CREATED_AT("created_at"),
        FLD_COORDINATES("coordinates"),
        FLD_PLACE("place"),
        FLD_RETWEET_CNT("retweet_count"),
        FLD_FOLLOWERS_WHEN_PUBLISHED("followers_when_published"),
        FLD_TWEET("tweet"),
        FLD_LANGUAGE("language"),
        FLD_PERMALINK("url"),
        FLD_EXTERNAL_LINKS("external_links"),
        FLD_ACCOUNT_NAME("account_name");
        private final String column;

        private TBL_TWITTER_POST(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POSTS_PER_HASHTAG {

        FLD_HASHTAG("hashtag"),
        FLD_CREATED_AT("created_at"),
        FLD_POST_ID("post_id"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LANGUAGE("language"),
        FLD_TWEET("tweet"),
        FLD_PERMALINK("url");

        private final String column;

        private TBL_TWITTER_POSTS_PER_HASHTAG(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POSTS_PER_EXTERNAL_URL {

        FLD_EXTERNAL_URL("external_url"),
        FLD_CREATED_AT("created_at"),
        FLD_POST_ID("post_id"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LANGUAGE("language"),
        FLD_TWEET("tweet"),
        FLD_PERMALINK("url");

        private final String column;

        private TBL_TWITTER_POSTS_PER_EXTERNAL_URL(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POSTS_PER_DATE {

        FLD_YEAR_MONTH_DAY_BUCKET("year_month_day_bucket"),
        FLD_CREATED_AT("created_at"),
        FLD_POST_ID("post_id"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LANGUAGE("language"),
        FLD_TWEET("tweet"),
        FLD_PERMALINK("url");

        private final String column;

        private TBL_TWITTER_POSTS_PER_DATE(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POSTS_PER_COORDINATE {

        FLD_GEO_BUCKET("geo_bucket"),
        FLD_CREATED_AT("created_at"),
        FLD_GEOLOCATION("geolocation"),
        FLD_POST_ID("post_id"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LANGUAGE("language"),
        FLD_TWEET("tweet"),
        FLD_PERMALINK("url");

        private final String column;

        private TBL_TWITTER_POSTS_PER_COORDINATE(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POSTS_PER_PLACE {

        FLD_PLACE_LITERAL("place_literal"),
        FLD_CREATED_AT("created_at"),
        FLD_PLACE("place"),
        FLD_POST_ID("post_id"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LANGUAGE("language"),
        FLD_TWEET("tweet"),
        FLD_PERMALINK("url");

        private final String column;

        private TBL_TWITTER_POSTS_PER_PLACE(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_POSTS_PER_ENGINE {

        FLD_ENGINE_TYPE("engine_type"),
        FLD_ENGINE_ID("engine_id"),
        FLD_POST_ID("post_id");

        private final String column;

        private TBL_TWITTER_POSTS_PER_ENGINE(String columnn) {
            this.column = columnn;
        }
    }

    enum TBL_TWITTER_LOG {

        FLD_ENGINE_TYPE("engine_type"),
        FLD_ENGINE_ID("engine_id"),
        FLD_STARTED("started"),
        FLD_ENDED("ended");

        private final String column;

        private TBL_TWITTER_LOG(String columnn) {
            this.column = columnn;
        }
    }

}
