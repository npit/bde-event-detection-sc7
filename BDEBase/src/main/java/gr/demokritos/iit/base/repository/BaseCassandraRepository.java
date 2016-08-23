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
package gr.demokritos.iit.base.repository;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.factory.BaseFactory;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.TableUtil;
import gr.demokritos.iit.base.util.Utils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;

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
    public Collection<Map<String, Object>> loadAllTweets(int atmost) {
        System.out.println("loading tweets...");

        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        if (atmost > 0)
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_DATE.getTableName())
                    .limit(atmost);
        else
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_DATE.getTableName())
                    ;

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
        System.out.println(String.format("loaded %d tweets", out.size()));
        return Collections.unmodifiableCollection(out);
    }


    @Override
    public Collection<Map<String, Object>> loadAllArticles(int atmost) {
        System.out.println("loading articles...");
        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        if (atmost > 0)
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                    .limit(atmost)
                    ;
        else
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                    ;
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
            String title = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName());
            res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(), title);
            // append
            //assert (published >= from) : String.format("query totally wrong: published=%d < from=%d", published, from);
            out.add(res);
        }
        // debug
        System.out.println(String.format("loaded %d articles", out.size()));
        // debug
        return Collections.unmodifiableCollection(out);
    }


    @Override
    public Collection<Map<String, Object>> loadTweets(long from) {
        System.out.println("loading tweets...");
        List<String> ymdLitRange = Utils.extractYearMonthDayLiteralRangeFrom(from);
        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        boolean got_first = false;
        for (String each_key : ymdLitRange) {
            if (got_first) {
                select
                        = QueryBuilder
                        .select()
                        .all()
                        .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_DATE.getTableName())
                        .where(eq(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), each_key));
            } else {
                select
                        = QueryBuilder
                        .select()
                        .all()
                        .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POSTS_PER_DATE.getTableName())
                        .where(eq(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), each_key))
                        .and(gte(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName(), from));
                got_first = true;
            }
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
                assert (created_at >= from) : String.format("query totally wrong: created_at=%d < from=%d", created_at, from);
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
        System.out.println("Extracted range from " + Long.toString(from) + " : ");
        Statement select;
        ResultSet results;
        Collection<Map<String, Object>> out = new ArrayList();
        boolean got_first = false;
        for (String each_key : ymdLitRange) {
            System.out.println(each_key);
            if (got_first) {
                select = QueryBuilder
                        .select()
                        .all()
                        .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                        .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), each_key));
            } else {
                select = QueryBuilder
                        .select()
                        .all()
                        .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                        .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), each_key))
                        .and((gte(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), from)));
                got_first = true;
            }
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
                String title = row.getString(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(), title);
                // append
                assert (published >= from) : String.format("query totally wrong: published=%d < from=%d", published, from);
                out.add(res);
            }
        }
        // debug
        System.out.println(String.format("loaded %d articles", out.size()));
        // debug
        return Collections.unmodifiableCollection(out);
    }

    /**
     * a more generic way of loading data
     */
    private static final Map<DataType.Name, Class> PLAIN_MAPPINGS = new HashMap();

    static {
        PLAIN_MAPPINGS.put(DataType.Name.VARCHAR, String.class);
        PLAIN_MAPPINGS.put(DataType.Name.TEXT, String.class);
        PLAIN_MAPPINGS.put(DataType.Name.BIGINT, Long.class);
        PLAIN_MAPPINGS.put(DataType.Name.FLOAT, Float.class);
        PLAIN_MAPPINGS.put(DataType.Name.DOUBLE, Double.class);
        PLAIN_MAPPINGS.put(DataType.Name.TIMESTAMP, Date.class);
//        PLAIN_MAPPINGS.put(DataType.Name.SET, Set.class); // Codec not found for requested operation: [set<varchar> <-> java.util.Set]
                                                            // need to explicitly set with TypeToken
//        PLAIN_MAPPINGS.put(DataType.Name.LIST, List.class);
//        PLAIN_MAPPINGS.put(DataType.Name.MAP, Map.class);
    }

    @Override
    public Map<String, Object> loadArticle(String permalink) {
        Map<String, Object> res = new HashMap();
        Statement select;
        try {
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES.getTableName())
                    .where(eq(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), permalink))
                    .and(eq(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(), TableUtil.extractReversedHost(permalink)))
                    .limit(1);
            ResultSet results = session.execute(select);
            Row row = results.one();
            if (row != null) {
                String reversed_host = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(), reversed_host);
                String entry_url = row.getString(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), entry_url);
                long published = row.getLong(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName());
                res.put(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName(), published);
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
            }
        } catch (Exception ex) {
            Logger.getLogger(BaseCassandraRepository.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        return res;
    }

    @Override
    public Map<String, Object> loadTweet(long post_id) {
        Map<String, Object> res = new HashMap();
        Statement select;
        try {
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Twitter.Tables.TWITTER_POST.getTableName())
                    .where(eq(Cassandra.Twitter.TBL_TWITTER_POST.FLD_POST_ID.getColumnName(), post_id))
                    .limit(1);
            ResultSet results = session.execute(select);
            Row row = results.one();
            if (row != null) {
                res = extractPlainColumns(row);
            }
        } catch (Exception ex){
            Logger.getLogger(BaseCassandraRepository.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        return res;
    }

    /**
     * extract the plain columns from a row. This method ignores collections or frozen types
     *
     * @param row
     * @return
     */
    private Map<String,Object> extractPlainColumns(Row row) {
        if (row == null) {
            return Collections.EMPTY_MAP;
        }
        Map<String, Object> res = new HashMap();
        ColumnDefinitions defs = row.getColumnDefinitions();
        for (ColumnDefinitions.Definition def : defs) {
            String name = def.getName();
            DataType type = def.getType();
            if (!type.isCollection() && !type.isFrozen()) {
                DataType.Name name1 = type.getName();
//                System.out.println("definition name: " + name + ", type.name: " + name1);
                Class name_class = PLAIN_MAPPINGS.get(name1);
                Object o = row.get(name, name_class);
                res.put(name, o);
            }
        }
        return res;
    }

    public static void main(String[] args) {
        IBaseConf co = new BaseConfiguration("../BDELocationExtraction/res/location_extraction.properties");
        BaseFactory bf = new BaseFactory(co);
        IBaseRepository re = bf.createBaseCassandraRepository();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_YEAR, Calendar.DAY_OF_YEAR - 20);
//        Collection<Map<String, Object>> loadArticles = re.loadArticles(now.getTimeInMillis());
//        for (Map<String, Object> loadArticle : loadArticles) {
//            System.out.println(loadArticle.get("title"));
//        }
//        Collection<Map<String, Object>> tweets = re.loadTweets(now.getTimeInMillis());
//        int limit = 10;
//        for (Map<String, Object> each : tweets) {
//            System.out.println(each.get("created_at") + ": " + each.get("tweet") + ", " + each.get("account_name") + ", " + each.get("language"));
//
//            if (limit-- == 0) {
//                break;
//            }
//        }
        Map<String, Object> tweet = re.loadTweet(707195717048119296l);
        for (Map.Entry<String, Object> each : tweet.entrySet()) {
            System.out.println(each.getKey() + ": " + each.getValue());
        }
    }
}
