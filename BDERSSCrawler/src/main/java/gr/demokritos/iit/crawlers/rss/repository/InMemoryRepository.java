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

import com.google.common.collect.Lists;
import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.crawlers.rss.extractors.SimpleExtractor;
import gr.demokritos.iit.crawlers.rss.model.Content;
import gr.demokritos.iit.crawlers.rss.model.CrawlId;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.model.UrlMetaData;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryRepository implements IRepository {

    private final Map<Item, Content> feedItemToEntryContent;
    private final Map<String, UrlMetaData> feedUrlToMetaData;
    private final Map<String, List<String>> entryUrlToData;
    private final BoilerpipeExtractor extractor;
    private CrawlId savedCrawlId;
    private Content lastContentSaved;

    public InMemoryRepository() {
        this.feedItemToEntryContent = new HashMap();
        this.feedUrlToMetaData = new HashMap();
        this.entryUrlToData = new HashMap();
        this.extractor = new SimpleExtractor();
    }

    @Override
    public List<SyndEntry> identifyNewEntries(Item item, List<SyndEntry> candidateEntries) {
        List<SyndEntry> entries = Lists.newArrayList();
        for (SyndEntry candidateEntry : candidateEntries) {
            if (!feedItemToEntryContent.containsKey(item)) {
                entries.add(candidateEntry);
            }
        }
        return entries;
    }

    @Override
    public void savePage(Item item, String title, Content content, Date publishedDate) throws IOException, BoilerpipeProcessingException {
        feedItemToEntryContent.put(item, content);
        lastContentSaved = content;

        List<String> data = Lists.newArrayList();
        data.add("" + item.getCrawlId().getId());
        data.add(item.getFeedUrl());
        String entryUrl = content.getUrl();
        data.add(entryUrl);

        data.add(content.getRawText());
        String cleanText = extractor.getText(content.getRawText());
        if (cleanText.isEmpty()) {
            cleanText = null;
        }
        data.add(cleanText);
        data.add(convertToString(content.getCrawlDate()));
        if (publishedDate == null) {
            data.add("" + AbstractRepository.MISSING_PUBLISHED_DATE);
        } else {
            data.add(convertToString(publishedDate));
        }
        // append title at the end.
        data.add(title);
        System.out.println("save: " + entryUrl + ", title: " + title);
        entryUrlToData.put(entryUrl, data);
    }

    private String convertToString(Date date) {
        return "" + date.getTime();
    }

    @Override
    public CrawlId findMostRecentCrawlId() {
        return savedCrawlId;
    }

    @Override
    public void saveCrawlId(CrawlId crawlId) {
        this.savedCrawlId = crawlId;
    }

    @Override
    public UrlMetaData getFeedMetadata(String url) {
        if (feedUrlToMetaData.containsKey(url)) {
            return feedUrlToMetaData.get(url);
        }
        return null;
    }

    @Override
    public void updateFeedMetaData(Content content) {
        UrlMetaData metaData = content.getUrlMetaData();
        this.feedUrlToMetaData.put(content.getUrl(), metaData);
    }

    @Override
    public List<String> find(String entryUrl) {
        return entryUrlToData.get(entryUrl);
    }

    public Content findLastContentSaved() {
        return lastContentSaved;
    }
}
