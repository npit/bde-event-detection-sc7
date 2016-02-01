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

import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.model.Content;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.model.UrlMetaData;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * User: ade
 *
 * A mechanism for storing crawled data A Repository should produce consistent
 * results when used by multiple threads at the same time. A Repository stores
 * metadata about each crawled feed and the contents of the html page associated
 * with each entry in crawled feed.
 */
public interface IRepository {

    int MISSING_PUBLISHED_DATE = -1;

    public List<SyndEntry> identifyNewEntries(Item item, List<SyndEntry> candidateEntries);

    void savePage(Item item, Content content, Date publishedDate) throws IOException, BoilerpipeProcessingException;

    CrawlId findMostRecentCrawlId();

    void saveCrawlId(CrawlId crawlId);

    UrlMetaData getFeedMetadata(String url);

    void updateFeedMetaData(Content content);

    List<String> find(String url);
}
