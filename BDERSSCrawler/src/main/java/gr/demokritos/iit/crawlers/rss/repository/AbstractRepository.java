/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.crawlers.rss.repository;

import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import gr.demokritos.iit.crawlers.rss.factory.conf.IRSSConf;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlStrategy;
import gr.demokritos.iit.crawlers.rss.model.Item;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public abstract class AbstractRepository {

    public static final int MISSING_PUBLISHED_DATE = -1;



    protected IRSSConf.OperationMode crawlMode;
    protected final CrawlStrategy crawlerStrategy;
    protected final BoilerpipeExtractor extractor;

    public AbstractRepository(CrawlStrategy crawlerStrategy, BoilerpipeExtractor extractor) {
        this.crawlerStrategy = crawlerStrategy;
        this.extractor = extractor;
    }

    /**
     * true if the URL provided does not exist in the repository
     *
     * @param link
     * @return
     */
    public abstract boolean isNewEntry(String link);

    protected long calculatePublishedValue(Date publishedDate) {
        // If we don't have a published date then we set the value to a special sentinel value.
        // This means we have to check for this sentinel value when retrieving data.
        if (publishedDate == null) {
            return MISSING_PUBLISHED_DATE;
        }
        return publishedDate.getTime();
    }

    protected List<SyndEntry> findNewEntries(Item item, List<SyndEntry> candidateEntries) {
        List<SyndEntry> newEntries = new ArrayList();
        for (SyndEntry candidateEntry : candidateEntries) {
            if (isNewEntry(candidateEntry.getLink())) {
                newEntries.add(candidateEntry);
            }
        }
        return newEntries;
    }
}
