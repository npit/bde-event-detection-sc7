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
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BaseCassandraRepository implements IBaseRepository {

    protected final Session session;

    public BaseCassandraRepository(Session session) {
        this.session = session;
    }

    @Override
    public Collection<Map<String, Object>> loadTweets(long from) {
        System.out.println("loading tweets...");
        List<String> ymdLitRange = Utils.extractYearMonthDayLiteralRangeFrom(from);
        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        for (String each_key : ymdLitRange) {
            select
                    = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_DATE.getTableName())
                    .where(eq(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), each_key))
                    .and(gte(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName(), from));
            results = session.execute(select);
            for (Row row : results) {
                Map<String, Object> res = new HashMap();
                String year_month_day_bucket = row.getString(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day_bucket);
                long created_at = row.getLong(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName(), created_at);
                long post_id = row.getLong(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName(), post_id);
                String account_name = row.getString(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.getColumnName(), account_name);
                String lang = row.getString(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.getColumnName(), lang);
                String tweet = row.getString(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName(), tweet);
                String permalink = row.getString(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.getColumnName());
                res.put(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.getColumnName(), permalink);
                // append
                out.add(res);
            }
        }
        System.out.println(String.format("loaded %d tweets", out.size()));
        return Collections.unmodifiableCollection(out);
    }

    @Override
    public Collection<Map<String, Object>> loadArticles(long from) {
        System.out.println("loading articles...");
        List<String> ymdLitRange = Utils.extractYearMonthDayLiteralRangeFrom(from);
        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        for (String each_key : ymdLitRange) {
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                    .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), each_key))
                    .and((gte(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), from)));
            results = session.execute(select);
            for (Row row : results) {
                Map<String, Object> res = new HashMap();
                String year_month_day_bucket = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day_bucket);
                long published = row.getLong(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), published);
                String entry_url = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), entry_url);
                Set<String> place_literal = row.getSet(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), String.class);
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), place_literal);
                String feed_url = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName(), feed_url);
                long crawl_id = row.getLong(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWL_ID.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWL_ID.getColumnName(), crawl_id);
                String raw_text = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.getColumnName(), raw_text);
                String clean_text = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(), clean_text);
                long crawled = row.getLong(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(), crawled);
                String lang = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.getColumnName(), lang);
                // append
                out.add(res);
            }
        }
        System.out.println(String.format("loaded %d articles", out.size()));
        return Collections.unmodifiableCollection(out);
    }

    public static void main(String[] args) {

        IBaseConf conf = new BaseConfiguration("~/Programming/Java/BigDataEurope/BDETwitterListener/res/twitter.properties");
        BaseFactory factory = new BaseFactory(conf);

        IBaseRepository repos = factory.createBaseCassandraRepository();

//        repos.loadTweets(timestamp_from)
    }

}
