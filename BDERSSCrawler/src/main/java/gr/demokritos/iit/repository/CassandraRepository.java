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
package gr.demokritos.iit.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.crawlers.schedule.CrawlStrategy;
import gr.demokritos.iit.crawlers.util.Utils;
import gr.demokritos.iit.model.Content;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.model.UrlMetaData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraRepository extends AbstractRepository implements IRepository {

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
    public void savePage(Item item, Content content, Date publishedDate) throws IOException, BoilerpipeProcessingException {
        System.out.println("save content: " + content.getUrl());
        Statement upsert;
        try {
            String cleanText = extractor.getText(content.getRawText());
            CrawlId crawlId = item.getCrawlId();
            long pub_date = calculatePublishedValue(publishedDate);
            boolean isNewEntry = isNewEntry(content.getUrl());
            // insert in base table.
            upsert = QueryBuilder
                    .update(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                    .with(set(TBL_ARTICLES.FLD_CRAWL_ID.columnn, crawlId.getId()))
                    .and(set(TBL_ARTICLES.FLD_FEED_URL.columnn, item.getFeedUrl()))
                    .and(set(TBL_ARTICLES.FLD_RAW_TEXT.columnn, content.getRawText()))
                    .and(set(TBL_ARTICLES.FLD_CLEAN_TEXT.columnn, cleanText))
                    .and(set(TBL_ARTICLES.FLD_CRAWLED.columnn, content.getCrawlDate().getTime()))
                    .and(set(TBL_ARTICLES.FLD_PUBLISHED.columnn, pub_date))
                    .and(set(TBL_ARTICLES.FLD_LANGUAGE.columnn, "")) // TODO identify language
                    .and(set(TBL_ARTICLES.FLD_PLACE_LITERAL.columnn, new HashSet<String>())) // TODO call Named Entity Extraction, and set keys from there.
                    .where(eq(TBL_ARTICLES.FLD_ENTRY_URL.columnn, content.getUrl()));
            session.execute(upsert);
            if (isNewEntry) {
                // insert in articles_per_date
                String year_month_day = Utils.extractYearMonthDayLiteral(publishedDate);
                Statement insert = QueryBuilder
                        .insertInto(session.getLoggedKeyspace(), crawlerStrategy.TableArticlesPerDate())
                        .value(TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.columnn, pub_date)
                        .value(TBL_ARTICLES_PER_DATE.FLD_FEED_URL.columnn, item.getFeedUrl())
                        .value(TBL_ARTICLES_PER_DATE.FLD_RAW_TEXT.columnn, content.getRawText())
                        .value(TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.columnn, cleanText)
                        .value(TBL_ARTICLES_PER_DATE.FLD_CRAWLED.columnn, content.getCrawlDate().getTime())
                        .value(TBL_ARTICLES_PER_DATE.FLD_LANGUAGE.columnn, "")
                        .value(TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.columnn, new HashSet<String>())
                        .value(TBL_ARTICLES_PER_DATE.FLD_YEAR_MONTH_DAY_BUCKET.columnn, year_month_day)
                        .value(TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.columnn, content.getUrl());
                session.execute(insert);
            } else {
                // TODO: get published date
                // compare and if different remove and insert new.
                // OR: update content and keep old date (faster)
            }
            // TODO use saving policy for _per_place
//            // insert in articles_per_place
//            upsert = QueryBuilder
//                    .update(session.getLoggedKeyspace(), crawlerStrategy.TableArticlesPerPlace())
//                    .with(set(TBL_ARTICLES_PER_PLACE.FLD_PUBLISHED.columnn, pub_date))
//                        .and(set(TBL_ARTICLES_PER_PLACE.FLD_FEED_URL.columnn, item.getFeedUrl()))
//                        .and(set(TBL_ARTICLES_PER_PLACE.FLD_RAW_TEXT.columnn, content.getRawText()))
//                        .and(set(TBL_ARTICLES_PER_PLACE.FLD_CLEAN_TEXT.columnn, cleanText))
//                        .and(set(TBL_ARTICLES_PER_PLACE.FLD_CRAWLED.columnn, content.getCrawlDate().getTime()))
//                        .and(set(TBL_ARTICLES_PER_PLACE.FLD_LANGUAGE.columnn, "")) // TODO identify language
//                    .where(eq(TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.columnn, "")) // TODO add place literal KEY
//                    .and(eq(TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.columnn, content.getUrl()));
//            session.execute(upsert);
        } catch (BoilerpipeProcessingException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex1) {
            throw new RuntimeException(ex1);
        }
    }

    @Override
    public CrawlId findMostRecentCrawlId() {
        String key = TBL_CRAWLS.FLD_CRAWL_TYPE.columnn;
        String id = TBL_CRAWLS.FLD_CRAWL_ID.columnn;
        String start = TBL_CRAWLS.FLD_START.columnn;
        String end = TBL_CRAWLS.FLD_END.columnn;

        Statement select = QueryBuilder
                .select(id, start, end)
                .from(session.getLoggedKeyspace(), crawlerStrategy.TableCrawls())
                .where(eq(key, crawlerStrategy.crawlType()))
                .limit(1);
        ResultSet results = session.execute(select);
        Row one = results.one();

        if (one != null) {
            long max_existing = one.getLong(id);
            long started = one.getTimestamp(start).getTime();
            long ended = one.getTimestamp(end).getTime();
            return new CrawlId(max_existing, started, ended);
        }
        return null;
    }

    @Override
    public void saveCrawlId(CrawlId crawlId) {
        String key = TBL_CRAWLS.FLD_CRAWL_TYPE.columnn;
        String id = TBL_CRAWLS.FLD_CRAWL_ID.columnn;
        String start = TBL_CRAWLS.FLD_START.columnn;
        String end = TBL_CRAWLS.FLD_END.columnn;
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), crawlerStrategy.TableCrawls())
                .with(set(start, crawlId.getStartTimestamp())).and(set(end, crawlId.getEndTimestamp()))
                .where(eq(key, crawlerStrategy.crawlType()))
                .and(eq(id, crawlId.getId()));
        session.execute(upsert);
    }

    @Override
    public UrlMetaData getFeedMetadata(String url) {
        UrlMetaData metadata = null;

        String feed_url_key = TBL_FEEDS.FLD_FEED_URL.columnn;
        String etag_key = TBL_FEEDS.FLD_ETAG.columnn;
        String last_modified_key = TBL_FEEDS.FLD_LAST_MODIFIED.columnn;

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
        String feed_url_key = TBL_FEEDS.FLD_FEED_URL.columnn;
        String etag_key = TBL_FEEDS.FLD_ETAG.columnn;
        String last_modified_key = TBL_FEEDS.FLD_LAST_MODIFIED.columnn;
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), crawlerStrategy.TableFeeds())
                .with(set(etag_key, content.getEtag())).and(set(last_modified_key, content.getLastModified()))
                .where(eq(feed_url_key, content.getUrl()));
        session.execute(upsert);
    }

    @Override
    public List<String> find(String url) { // TODO implement a method to get published date from existing URL in DB
        // TODO: we need in articles_per_date, the date DESC ordered as CLUSTERING column, so we cannot update on the fly.
        Statement select = QueryBuilder
                .select(
                        TBL_ARTICLES.FLD_ENTRY_URL.columnn, TBL_ARTICLES.FLD_PUBLISHED.columnn
                )
                .from(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                .where(eq(TBL_ARTICLES.FLD_ENTRY_URL.columnn, url));
        ResultSet results = session.execute(select);

        Row one = results.one();
        if (one != null) {
            String sn = one.getString(TBL_ARTICLES.FLD_ENTRY_URL.columnn);
            long pub = one.getLong(TBL_ARTICLES.FLD_PUBLISHED.columnn);
            if (sn != null && !sn.isEmpty()) {
                List<String> res = new ArrayList(2);
                res.add(sn);
//                res.add(String.val)
                return res;
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isNewEntry(String link) {
        String key = TBL_ARTICLES.FLD_ENTRY_URL.columnn;
        Statement select = QueryBuilder
                .select(key)
                .from(session.getLoggedKeyspace(), crawlerStrategy.TableArticles())
                .where(eq(key, link))
                .limit(1);
        ResultSet results = session.execute(select);

        Row one = results.one();
        if (one != null) {
            String sn = one.getString(key);
            if (sn != null && !sn.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    enum TBL_ARTICLES {

        FLD_ENTRY_URL("entry_url"),
        FLD_PUBLISHED("published"),
        FLD_PLACE_LITERAL("place_literal"),
        FLD_FEED_URL("feed_url"),
        FLD_CRAWL_ID("crawl_id"),
        FLD_RAW_TEXT("raw_text"),
        FLD_CLEAN_TEXT("clean_text"),
        FLD_CRAWLED("crawled"),
        FLD_LANGUAGE("language");

        private final String columnn;

        private TBL_ARTICLES(String columnn) {
            this.columnn = columnn;
        }
    }

    enum TBL_ARTICLES_PER_DATE {

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

        private TBL_ARTICLES_PER_DATE(String columnn) {
            this.columnn = columnn;
        }
    }

    enum TBL_ARTICLES_PER_PLACE {

        FLD_PLACE_LITERAL("place_literal"),
        FLD_PUBLISHED("published"),
        FLD_ENTRY_URL("entry_url"),
        FLD_PLACE("tplace"),
        FLD_FEED_URL("feed_url"),
        FLD_CRAWL_ID("crawl_id"),
        FLD_RAW_TEXT("raw_text"),
        FLD_CLEAN_TEXT("clean_text"),
        FLD_CRAWLED("crawled"),
        FLD_LANGUAGE("language");

        private final String columnn;

        private TBL_ARTICLES_PER_PLACE(String columnn) {
            this.columnn = columnn;
        }
    }

    enum TBL_FEEDS {

        FLD_FEED_URL("feed_url"),
        FLD_ETAG("etag"),
        FLD_LAST_MODIFIED("last_modified");

        private final String columnn;

        private TBL_FEEDS(String columnn) {
            this.columnn = columnn;
        }
    }

    enum TBL_CRAWLS {

        FLD_CRAWL_TYPE("crawl_type"),
        FLD_CRAWL_ID("crawl_id"),
        FLD_START("start"),
        FLD_END("end");

        private final String columnn;

        private TBL_CRAWLS(String columnn) {
            this.columnn = columnn;
        }
    }

}
