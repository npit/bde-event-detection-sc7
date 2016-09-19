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

import gr.demokritos.iit.crawlers.rss.event.EventSink;
import gr.demokritos.iit.crawlers.rss.load.LoadRegistry;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.model.CrawlId;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlExecutor;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;
import java.util.concurrent.BlockingQueue;

public class Consumer implements DescribableRunnable {

    private final BlockingQueue<Item> queue;
    private final CrawlExecutor executor;
    private final LoadRegistry loadRegistry;
    private final IRepository repository;
    private final EventSink eventSink;

    private final boolean bForever;

    public Consumer(BlockingQueue<Item> queue, CrawlExecutor executor, LoadRegistry loadRegistry,
            IRepository repository, EventSink eventSink, boolean bRunForever) {
        this.queue = queue;
        this.executor = executor;
        this.loadRegistry = loadRegistry;
        this.repository = repository;
        this.eventSink = eventSink;
        this.bForever = bRunForever;
    }

    @Override
    public void run() {
        eventSink.startingToConsumeScheduledUrls();

        try {

            boolean bListNotEmpty = true;

            while (bListNotEmpty) {
                Item item = queue.take();

                CrawlId crawlId = item.getCrawlId();
                if (startSentinel(item)) {
                    eventSink.startedCrawling(crawlId);
                    loadRegistry.startedCrawl(crawlId);

                } else if (endSentinel(item)) {
                    //INVARIANT: There should only ever be one crawl cycle active at a time
                    //Thus we block new cycles from starting until the old one has finished.
                    //This runs the risk of propagating deadlock but greatly simplifies many other aspects of the system
                    eventSink.prepareToFinishCrawling(crawlId);
                    loadRegistry.blockUntilComplete(crawlId);//This blocks until the crawl is finished

                    //Report to downstream systems that crawling for this crawlId has finished
                    crawlId.finishedCrawling();
                    repository.saveCrawlId(crawlId);
                    eventSink.finishedCrawling(crawlId);
                    // if we want to exit operation
                    if (!bForever) {
                        bListNotEmpty = false;
                    }
                } else {
                    executor.executeWith(item);
                }
            }
        } catch (InterruptedException e) {
            eventSink.threadInterruptedError(e);
        }
    }

    private boolean startSentinel(Item item) {
        return START_SENTINEL.equals(item.getFeedUrl());
    }

    private boolean endSentinel(Item item) {
        return END_SENTINEL.equals(item.getFeedUrl());
    }

    @Override
    public String description() {
        return this.getClass().getSimpleName();
    }
}
