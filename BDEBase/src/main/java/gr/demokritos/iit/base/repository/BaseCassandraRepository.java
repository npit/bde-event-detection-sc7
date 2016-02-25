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
import gr.demokritos.iit.base.util.TableUtil;
import gr.demokritos.iit.base.util.Utils;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        boolean got_first = false;
        for (String each_key : ymdLitRange) {
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
//                ColumnDefinitions defs = row.getColumnDefinitions();
//                for (ColumnDefinitions.Definition def : defs) {
//                    String name = def.getName();
//                    DataType type = def.getType();
//                    DataType.Name name1 = type.getName();
//                    System.out.println("definition name: " + name + ", type.name: " + name1);
//                    res.put(name, row.get(name, MAPPINGS.get(name1)));
//                    out.add(res);
//                }
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
                out.add(res);
            }
        }
        // debug
        System.out.println(String.format("loaded %d articles", out.size()));
        // debug
        return Collections.unmodifiableCollection(out);
    }

    private static final Map<String, Class> MAPPINGS = new HashMap();

    static {
        MAPPINGS.put("varchar", String.class);
        MAPPINGS.put("bigint", Long.class);
        MAPPINGS.put("set", Set.class);
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
        } catch (MalformedURLException ex) {
            Logger.getLogger(BaseCassandraRepository.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        return res;
    }

    public static void main(String[] args) {
        IBaseConf co = new BaseConfiguration("../BDELocationExtraction/res/location_extraction.properties");
        BaseFactory bf = new BaseFactory(co);
        IBaseRepository re = bf.createBaseCassandraRepository();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_YEAR, Calendar.DAY_OF_YEAR - 10);
        Collection<Map<String, Object>> loadArticles = re.loadArticles(now.getTimeInMillis());
        for (Map<String, Object> loadArticle : loadArticles) {
            System.out.println(loadArticle.get("title"));
        }

    }
}
