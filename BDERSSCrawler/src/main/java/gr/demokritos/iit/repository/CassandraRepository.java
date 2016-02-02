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

import com.datastax.driver.core.Session;
import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.crawlers.schedule.CrawlStrategy;
import gr.demokritos.iit.model.Content;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.model.UrlMetaData;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraRepository implements IRepository {

    private final Session session;
    private final CrawlStrategy crawlerStrategy;
    private final BoilerpipeExtractor extractor;

    public static IRepository createBlogRepository(Session session) {
        return new CassandraRepository(session, CrawlStrategy.BLOG, CrawlStrategy.BLOG.extractor());
    }

    public static IRepository createNewsRepository(Session session) {
        return new CassandraRepository(session, CrawlStrategy.NEWS, CrawlStrategy.NEWS.extractor());
    }

    private CassandraRepository(Session session, CrawlStrategy strategy, BoilerpipeExtractor extractor) {
        this.session = session;
        this.crawlerStrategy = strategy;
        this.extractor = extractor;
    }

    @Override
    public List<SyndEntry> identifyNewEntries(Item item, List<SyndEntry> candidateEntries) {
        System.out.println("identify new entries: " + item.toString());
        return Collections.EMPTY_LIST;
    }

    @Override
    public void savePage(Item item, Content content, Date publishedDate) throws IOException, BoilerpipeProcessingException {
        System.out.println("savePage " + content.toString());
    }

    @Override
    public CrawlId findMostRecentCrawlId() {
        System.out.println("findMostRecentCrawlID");
        return new CrawlId(0l);
    }

    @Override
    public void saveCrawlId(CrawlId crawlId) {
        System.out.println("saveCrawlID" + crawlId.toString());
    }

    @Override
    public UrlMetaData getFeedMetadata(String url) {
        System.out.println("getFeedMetadata " + url);
        return null;
    }

    @Override
    public void updateFeedMetaData(Content content) {
        System.out.println("updateFeedMetaData: " + content.toString());
    }

    @Override
    public List<String> find(String url) {
        System.out.println("find " + url);
        return Collections.EMPTY_LIST;
    }

}
