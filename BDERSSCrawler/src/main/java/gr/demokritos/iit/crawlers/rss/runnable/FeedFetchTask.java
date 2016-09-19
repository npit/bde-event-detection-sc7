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
package gr.demokritos.iit.crawlers.rss.runnable;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.crawlers.rss.Fetcher;
import gr.demokritos.iit.crawlers.rss.event.EventSink;
import gr.demokritos.iit.crawlers.rss.model.Content;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class FeedFetchTask implements DescribableRunnable {

    private final Item item;
    private final Fetcher fetcher;
    private final IRepository repository;
    private final EventSink eventSink;

    public FeedFetchTask(Item item, Fetcher fetcher, IRepository repository, EventSink eventSink) {
        this.item = item;
        this.fetcher = fetcher;
        this.repository = repository;
        this.eventSink = eventSink;
    }

    @Override
    public void run() {
        eventSink.fetching(item);
        try {
            // fetch and parse the feed
            SyndFeedInput input = new SyndFeedInput();
            String url = item.getFeedUrl();
            Content contentFromFeed = fetcher.fetchUrl(url);
            //When fetching any url we must deal with the possibility that the conditional GET means we have no text.
            //This means that the feed has not been modified.
            if (contentFromFeed == null) {
                eventSink.finishedFetching(item, contentFromFeed);
                return;
            }

            // OBSOLETE
            //SyndFeed feed = input.build(new StringReader(contentFromFeed.getRawText()));
            // Create lenient reader
            InputStream is = new ByteArrayInputStream(contentFromFeed.getRawText().getBytes());
            System.out.println("input from url " + url);

            SyndFeed feed = input.build(new XmlReader(is, true));
            @SuppressWarnings("unchecked")
            List<SyndEntry> entries = feed.getEntries();

            // identify new entries
            List<SyndEntry> newEntries = repository.identifyNewEntries(item, entries);

            // fetch the html page for each new entry and save it
            for (SyndEntry newEntry : newEntries) {
                String permaLink = newEntry.getLink();
                Date publishedDate = newEntry.getPublishedDate();
                String title = newEntry.getTitle();
                try {
                    Content htmlContent = fetcher.fetchUrl(permaLink);
                    if (htmlContent == null) {
                        eventSink.error("Failed to fetch html content for : " + permaLink + " on item:" + item
                                + ". Continuing with next entry.");
                        continue;
                    }
                    if (title == null) {
                        title = "";
                    } else {
                        title = removeAscIIDeleteChar(title);
                    }
                    repository.savePage(item, title, htmlContent, publishedDate);
                } catch (IOException e) {
                    eventSink.error(permaLink, e);
                } catch (BoilerpipeProcessingException e) {
                    eventSink.error(permaLink, e);
                } catch (Exception e) {
                    eventSink.error(permaLink, e);
                }
            }

            //Save the metadata associated with the feed not the html page since later on we're interested in finding
            //out if the feed has changed rather than a particular html page.
            repository.updateFeedMetaData(contentFromFeed);
            eventSink.finishedFetching(item, contentFromFeed);
        } catch (FeedException | IOException e) {
            handleFetchError(item.getFeedUrl(), e);
        }
    }

    private void handleFetchError(String affectedUrl, Exception e) {
        String itemAndUrl = item.toString() + " : " + affectedUrl;
        eventSink.error(itemAndUrl, e);
    }

    @Override
    public String description() {
        return "FeedFetchTask<" + item + ">";
    }

    /**
     * reuters feed titles sometimes contain the ascii delete char, we do not
     * want it stored.
     *
     * @param str
     * @return
     */
    private String removeAscIIDeleteChar(String str) {
        return StringUtils.normalizeSpace(str.replaceAll("\\x7f", " "));
    }
}
