/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.base.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.factory.BaseFactory;
import gr.demokritos.iit.base.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BaseCassandraRepository implements IRepository {

    protected final Session session;

    public BaseCassandraRepository(Session session) {
        this.session = session;
    }

    @Override
    public Collection<Map<String, Object>> loadTweets(long from) {
        Statement select
                = QueryBuilder
                .select()
                .all()
                .from(session.getLoggedKeyspace(), Tables.TWITTER_POSTS_PER_DATE.table_name)
                .where(eq(TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn, Utils.extractYearMonthDayLiteral(from)))
                .and(gte(TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.columnn, from));
        ResultSet results;
        results = session.execute(select);
        Collection<Map<String, Object>> out = new ArrayList();
        for (Row row : results) {
            Map<String, Object> res = new HashMap();
            String year_month_day_bucket = row.getString(TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn);
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn, year_month_day_bucket);
            long created_at = row.getTimestamp(TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.columnn).getTime();
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.columnn, created_at);
            long post_id = row.getLong(TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.columnn);
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.columnn, post_id);
            String account_name = row.getString(TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.columnn);
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.columnn, account_name);
            String lang = row.getString(TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.columnn);
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.columnn, lang);
            String tweet = row.getString(TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.columnn);
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.columnn, tweet);
            String permalink = row.getString(TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.columnn);
            res.put(TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.columnn, permalink);
            // append
            out.add(res);
        }
        return Collections.unmodifiableCollection(out);
    }

    @Override
    public Collection<Map<String, Object>> loadArticles(long from) {
        Statement select
                = QueryBuilder
                .select()
                .all()
                .from(session.getLoggedKeyspace(), Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.table_name)
                .where(eq(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn, Utils.extractYearMonthDayLiteral(from)))
                .and(gte(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_PUBLISHED.columnn, from));
        ResultSet results;
        results = session.execute(select);
        Collection<Map<String, Object>> out = new ArrayList();
        for (Row row : results) {
            Map<String, Object> res = new HashMap();
            String year_month_day_bucket = row.getString(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn, year_month_day_bucket);
            long published = row.getTimestamp(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_PUBLISHED.columnn).getTime();
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_PUBLISHED.columnn, published);
            String entry_url = row.getString(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_ENTRY_URL.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_ENTRY_URL.columnn, entry_url);
            Set<String> place_literal = row.getSet(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_PLACE_LITERAL.columnn, String.class);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_PLACE_LITERAL.columnn, place_literal);
            String feed_url = row.getString(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_FEED_URL.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_FEED_URL.columnn, feed_url);
            long crawl_id = row.getLong(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_CRAWL_ID.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_CRAWL_ID.columnn, crawl_id);
            String raw_text = row.getString(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_RAW_TEXT.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_RAW_TEXT.columnn, raw_text);
            String clean_text = row.getString(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_CLEAN_TEXT.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_CLEAN_TEXT.columnn, clean_text);
            long crawled = row.getTimestamp(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_CRAWLED.columnn).getTime();
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_CRAWLED.columnn, crawled);
            String lang = row.getString(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_LANGUAGE.columnn);
            res.put(TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE.FLD_LANGUAGE.columnn, lang);
            // append
            out.add(res);
        }
        return Collections.unmodifiableCollection(out);
    }

    // we declare tables and needed fields here, as in the twitterListener/RSSCrawler respectively. 
    // We apply this inaccuracy in order to utilized from here only the things we need for loading data,
    // while keeping each module with his backend type independent.
    // cassandra tables
    enum Tables {

//        TWITTER_SOURCE("twitter_source"),
//        TWITTER_USER("twitter_user"),
//        TWITTER_POST("twitter_post"),
        TWITTER_POSTS_PER_DATE("twitter_posts_per_date"),
        //        TWITTER_POSTS_PER_HASHTAG("twitter_posts_per_hashtag"),
        //        TWITTER_POSTS_PER_EXTERNAL_URL("twitter_posts_per_external_url"),
        //        TWITTER_POSTS_PER_COORDINATES("twitter_posts_per_coordinates"),
        //        TWITTER_POSTS_PER_PLACE("twitter_posts_per_place"),
        //        TWITTER_POSTS_PER_ENGINE("twitter_posts_per_engine"),
        //        TWITTER_LOG("twitter_log");
        NEWS_ARTICLES_PER_PUBLISHED_DATE("news_articles_per_published_date");
        private final String table_name;

        private Tables(String table_name) {
            this.table_name = table_name;
        }

        public String getColumnName() {
            return table_name;
        }
    }

    public enum TBL_TWITTER_POSTS_PER_DATE {

        FLD_YEAR_MONTH_DAY_BUCKET("year_month_day_bucket"),
        FLD_CREATED_AT("created_at"),
        FLD_POST_ID("post_id"),
        FLD_ACCOUNT_NAME("account_name"),
        FLD_LANGUAGE("language"),
        FLD_TWEET("tweet"),
        FLD_PERMALINK("url");

        private final String columnn;

        private TBL_TWITTER_POSTS_PER_DATE(String columnn) {
            this.columnn = columnn;
        }

        public String getColumnName() {
            return columnn;
        }
    }

    public enum TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE {

        FLD_YEAR_MONTH_DAY_BUCKET("year_month_day_literal"),
        FLD_PUBLISHED("published"),
        FLD_ENTRY_URL("entry_url"),
        FLD_PLACE_LITERAL("place_literal"),
        FLD_FEED_URL("feed_url"),
        FLD_CRAWL_ID("crawl_id"),
        FLD_RAW_TEXT("raw_text"),
        FLD_CLEAN_TEXT("clean_text"),
        FLD_CRAWLED("crawled"),
        FLD_LANGUAGE("language");

        private final String columnn;

        private TBL_NEWS_ARTICLES_PER_PUBLISHED_DATE(String columnn) {
            this.columnn = columnn;
        }

        public String getColumnName() {
            return columnn;
        }

    }

    public static void main(String[] args) {

        IBaseConf conf = new BaseConfiguration("~/Programming/Java/BigDataEurope/BDETwitterListener/res/twitter.properties");
        BaseFactory factory = new BaseFactory(conf);

        IRepository repos = factory.createBaseCassandraRepository();

//        repos.loadTweets(timestamp_from)
    }

}
