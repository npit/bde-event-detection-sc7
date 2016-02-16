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
package gr.demokritos.iit.crawlers.rss.event;

import gr.demokritos.iit.crawlers.rss.model.Content;
import gr.demokritos.iit.crawlers.rss.model.CrawlId;
import gr.demokritos.iit.crawlers.rss.model.Item;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingEventSink implements EventSink {

    private static final Logger log = Logger.getLogger(LoggingEventSink.class.getName());

    @Override
    public void loadingSchedule() {
        log.info("Loading schedule");
    }

    @Override
    public void schedulingCrawl(CrawlId crawlId) {
        log.log(Level.INFO, "Scheduling crawl for crawl: {0}", crawlId);
    }

    @Override
    public void startingToConsumeScheduledUrls() {
        log.info("Starting to consume scheduled URLs for crawl");
    }

    @Override
    public void startedCrawling(CrawlId crawlId) {
        log.log(Level.INFO, "Start_Sentinel value received. Started crawling: {0}", crawlId);
    }

    @Override
    public void fetching(Item item) {
        log.log(Level.INFO, "Fetching: {0}", item);
    }

    @Override
    public void finishedFetching(Item item, Content contentFromFeed) {
        if (contentFromFeed == null) {
            log.log(Level.INFO, "Finished fetching: {0} It was unmodified.", item);
        } else {
            log.log(Level.INFO, "Finished fetching: {0} with metadata: {1}",
                    new Object[]{item, contentFromFeed.getUrlMetaData()});
        }
    }

    @Override
    public void prepareToFinishCrawling(CrawlId crawlId) {
        log.log(Level.INFO, "End_Sentinel value received. Preparing to finish crawling: {0}", crawlId);
    }

    @Override
    public void finishedCrawling(CrawlId crawlId) {
        log.log(Level.INFO, "Finished crawling: {0}", crawlId);
    }

    @Override
    public void threadInterruptedError(InterruptedException e) {
        log.severe(convertToString(e));
        //Restore this thread's interrupted status
        Thread.currentThread().interrupt();
    }

    @Override
    public void error(String message) {
        log.severe(message);
    }

    @Override
    public void error(Exception e) {
        error(convertToString(e));
    }

    @Override
    public void error(String message, Exception e) {
        error(message + " : " + convertToString(e));
    }

    @Override
    public void shutdown(String sTask) {
        log.info(String.format("Shutting down %s", sTask));
    }

    private String convertToString(Exception e) {
        // Converts an exception to a String. Handles corner cases like NullPointerExceptions without detail messages.
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        return w.toString();
    }
}
