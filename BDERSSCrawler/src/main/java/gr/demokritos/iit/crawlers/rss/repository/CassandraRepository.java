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
package gr.demokritos.iit.crawlers.rss.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlStrategy;
import gr.demokritos.iit.base.util.TableUtil;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.base.util.langdetect.CybozuLangDetect;
import gr.demokritos.iit.crawlers.rss.model.Content;
import gr.demokritos.iit.crawlers.rss.model.CrawlId;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.model.UrlMetaData;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraRepository extends AbstractRepository implements IRepository {

    private static final long NOT_EXISTING_ARTICLE = -2l; // set it lower than MISSING_PUB_DATE
    private final Session session;

    public static IRepository createBlogRepository(Session session) {
        return new CassandraRepository(session, CrawlStrategy.BLOG, CrawlStrategy.BLOG.extractor());
    }

    public static IRepository createNewsRepository(Session session) {
        return new CassandraRepository(session, CrawlStrategy.NEWS, CrawlStrategy.NEWS.extractor());
    }

    private CassandraRepository(Session session, CrawlStrategy strategy, BoilerpipeExtractor extractor) {
        super(strategy, extractor);
        this.session = session;
    }

    @Override
    public List<SyndEntry> identifyNewEntries(Item item, List<SyndEntry> candidateEntries) {
        return findNewEntries(item, candidateEntries);
    }

    @Override
    public void savePage(Item item, String title, Content content, Date publishedDate) throws IOException, BoilerpipeProcessingException {
        long pub_date = calculatePublishedValue(publishedDate);
        long existing_pub_date = getPublishedDateIfExisting(content.getUrl());
        String year_month_day = Utils.extractYearMonthDayLiteral(publishedDate);
        // if article exists and is updated
        if ((NOT_EXISTING_ARTICLE != existing_pub_date) && (MISSING_PUBLISHED_DATE != pub_date) && (pub_date > existing_pub_date)) {
            // we need to specifically delete before inserting/updating cause pub_date is a clustering column
            deletePage(content.getUrl(), year_month_day);
        }
        insertPage(item, title, content, pub_date, year_month_day);
    }

    private void insertPage(Item item, String title, Content content, long pub_date, String year_month_day) throws BoilerpipeProcessingException, MalformedURLException {
//        System.out.println("Inserting page: " + content.getUrl()); // debug
        CrawlId crawlId = item.getCrawlId();
        String cleanText = extractor.getText(content.getRawText());
        // identify language
        String lang;
        if (cleanText == null || cleanText.isEmpty()) {
            cleanText = "";
            lang = CybozuLangDetect.UNDEFINED_LANG;
        } else {
            lang = CybozuLangDetect.getInstance().identifyLanguage(cleanText);
        }
        long crawled_timestamp = content.getCrawlDate().getTime();
        // a Named Entity place holder (for location)
        Set<String> named_entities = new HashSet();

        Statement insert;
        // insert in base table.
        insert = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(), TableUtil.getReversedHost(TableUtil.reverseUrl(content.getUrl())))
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), content.getUrl())
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName(), pub_date)
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWL_ID.getColumnName(), crawlId.getId())
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_FEED_URL.getColumnName(), item.getFeedUrl())
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_RAW_TEXT.getColumnName(), content.getRawText())
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_CLEAN_TEXT.getColumnName(), cleanText)
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName(), crawled_timestamp)
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_LANGUAGE.getColumnName(), lang)
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_PLACE_LITERAL.getColumnName(), named_entities)
                .value(Cassandra.RSS.TBL_ARTICLES.FLD_TITLE.getColumnName(), title);
        session.execute(insert);
        // insert in articles_per_published_date
        insert = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), crawlerStrategy.TableArticlesPerPublishedDate())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), pub_date)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), content.getUrl())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName(), item.getFeedUrl())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.getColumnName(), content.getRawText())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(), cleanText)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(), crawled_timestamp)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.getColumnName(), lang)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), named_entities)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(), title);
        session.execute(insert);
        // insert in articles_per_crawled_date
        insert = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), crawlerStrategy.TableArticlesPerCrawledDate())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(), crawled_timestamp)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(), pub_date)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), content.getUrl())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName(), item.getFeedUrl())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.getColumnName(), content.getRawText())
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(), cleanText)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.getColumnName(), lang)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName(), named_entities)
                .value(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(), title);
        session.execute(insert);
        // insert in articles_per_place gets place in LocationExtraction module
    }

    @Override
    public CrawlId findMostRecentCrawlId() {
        String key = Cassandra.RSS.TBL_CRAWLS.FLD_CRAWL_TYPE.getColumnName();
        String id = Cassandra.RSS.TBL_CRAWLS.FLD_CRAWL_ID.getColumnName();
        String start = Cassandra.RSS.TBL_CRAWLS.FLD_START.getColumnName();
        String end = Cassandra.RSS.TBL_CRAWLS.FLD_END.getColumnName();

        Statement select = QueryBuilder
                .select(id, start, end)
                .from(session.getLoggedKeyspace(), crawlerStrategy.TableCrawls())
                .where(eq(key, crawlerStrategy.crawlType()))
                .limit(1);
        ResultSet results = null;
        try {
            results = session.execute(select);
        }
        catch(QueryExecutionException ex)
        {
            String errorMessage = ex.getMessage();
            ex.printStackTrace();
            System.err.println("QueryExecutionException : " + errorMessage);
            return null;
        }
        catch(QueryValidationException ex)
        {
            String errorMessage = ex.getMessage();
            ex.printStackTrace();
            System.err.println("QueryValidation : " + errorMessage);
            return null;
        }
        catch(NoHostAvailableException ex)
        {
            String errorMessage = ex.getMessage();
            ex.printStackTrace();
            System.err.println("NoHostAvailableException : " + errorMessage);
            return null;
        }

        Row one = results.one();

        if (one != null) {
            long max_existing = one.getLong(id);
            long started = one.getLong(start);
            long ended = one.getLong(end);
            return new CrawlId(max_existing, started, ended);
        }
        return null;
    }

    @Override
    public void saveCrawlId(CrawlId crawlId) {
        String key = Cassandra.RSS.TBL_CRAWLS.FLD_CRAWL_TYPE.getColumnName();
        String id = Cassandra.RSS.TBL_CRAWLS.FLD_CRAWL_ID.getColumnName();
        String start = Cassandra.RSS.TBL_CRAWLS.FLD_START.getColumnName();
        String end = Cassandra.RSS.TBL_CRAWLS.FLD_END.getColumnName();
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), crawlerStrategy.TableCrawls())
                .with(set(start, crawlId.getStartTimestamp())).and(set(end, crawlId.getEndTimestamp()))
                .where(eq(key, crawlerStrategy.crawlType()))
                .and(eq(id, crawlId.getId()));
        try
        {
            session.execute(upsert);
        }
        catch(QueryExecutionException ex)
        {
            String errorMessage = ex.getMessage();
            System.err.println("QueryExecutionException : " + errorMessage);
            return;
        }
        catch(QueryValidationException ex)
        {
            String errorMessage = ex.getMessage();
            System.err.println("QueryValidation : " + errorMessage);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        catch(NoHostAvailableException ex)
        {
            String errorMessage = ex.getMessage();
            System.err.println("NoHostExc : " + errorMessage);
            return;
        }
    }

    @Override
    public UrlMetaData getFeedMetadata(String url) {
        UrlMetaData metadata = null;

        String feed_url_key = Cassandra.RSS.TBL_FEEDS.FLD_FEED_URL.getColumnName();
        String etag_key = Cassandra.RSS.TBL_FEEDS.FLD_ETAG.getColumnName();
        String last_modified_key = Cassandra.RSS.TBL_FEEDS.FLD_LAST_MODIFIED.getColumnName();

        Statement select = QueryBuilder
                .select(etag_key, last_modified_key)
                .from(session.getLoggedKeyspace(), crawlerStrategy.TableFeeds())
                .where(eq(feed_url_key, url))
                .limit(1);
        ResultSet results = session.execute(select);
        Row one = results.one();

        if (one != null) {
            String etag = one.getString(etag_key);
            String last_modified = one.getString(last_modified_key);
            metadata = new UrlMetaData(etag, last_modified);
        }
        return metadata;
    }

    @Override
    public void updateFeedMetaData(Content content) {
        String feed_url_key = Cassandra.RSS.TBL_FEEDS.FLD_FEED_URL.getColumnName();
        String etag_key = Cassandra.RSS.TBL_FEEDS.FLD_ETAG.getColumnName();
        String last_modified_key = Cassandra.RSS.TBL_FEEDS.FLD_LAST_MODIFIED.getColumnName();
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), crawlerStrategy.TableFeeds())
                .with(set(etag_key, content.getEtag())).and(set(last_modified_key, content.getLastModified()))
                .where(eq(feed_url_key, content.getUrl()));
        session.execute(upsert);
    }

    @Override
    public List<String> find(String url) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * return the published date of the article if existing.
     *
     * @param url
     * @return
     */
    private long getPublishedDateIfExisting(String url) throws MalformedURLException {
        // we need in articles_per_date, the date DESC ordered as CLUSTERING column, so we cannot update on the fly.
        Statement select = QueryBuilder
                .select(
                        Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName()
                )
                .from(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                .where(eq(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(), TableUtil.getReversedHost(TableUtil.reverseUrl(url))))
                .and(eq(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), url));
        ResultSet results = session.execute(select);

        Row one = results.one();
        if (one != null) {
            long pub = one.getLong(Cassandra.RSS.TBL_ARTICLES.FLD_PUBLISHED.getColumnName());
            return pub;
        }

        return NOT_EXISTING_ARTICLE;
    }

    @Override
    public boolean isNewEntry(String link) {
        Statement select;
        try {
            select = QueryBuilder
                    .select(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName())
                    .from(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                    .where(eq(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), link))
                    .and(eq(Cassandra.RSS.TBL_ARTICLES.FLD_REVERSED_HOST.getColumnName(), TableUtil.getReversedHost(TableUtil.reverseUrl(link))))
                    .limit(1);
            ResultSet results = session.execute(select);

            Row one = results.one();
            if (one != null) {
                String sn = one.getString(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName());
                if (sn != null && !sn.isEmpty()) {
                    return false;
                }
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }

    private void deletePage(String url, String year_month_day) {
//        System.out.println("delete content: " + url); //debug
        Statement delete;
        // delete from base table.
        delete = QueryBuilder
                .delete().all().from(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                .where(eq(Cassandra.RSS.TBL_ARTICLES.FLD_ENTRY_URL.getColumnName(), url));
        session.execute(delete);
        // delete from articles per published date table
        delete = QueryBuilder
                .delete().all().from(session.getLoggedKeyspace(), crawlerStrategy.TableArticlesPerPublishedDate())
                .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day))
                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), url));
        session.execute(delete);
        // delete from articles per crawled date table
        delete = QueryBuilder
                .delete().all().from(session.getLoggedKeyspace(), crawlerStrategy.TableArticlesPerCrawledDate())
                .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.getColumnName(), year_month_day))
                .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(), url));
        session.execute(delete);
    }
}
