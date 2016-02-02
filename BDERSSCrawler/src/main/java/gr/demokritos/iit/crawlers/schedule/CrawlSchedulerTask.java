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
package gr.demokritos.iit.crawlers.schedule;

import gr.demokritos.iit.crawlers.event.EventSink;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.runnable.DescribableRunnable;
import java.util.concurrent.BlockingQueue;

public class CrawlSchedulerTask implements DescribableRunnable {

    private final CrawlSchedule crawlSchedule;
    private final BlockingQueue<Item> queue;
    private final CrawlIdGenerator generator;
    private final EventSink eventSink;

    public boolean isFinished = false;

    public CrawlSchedulerTask(CrawlSchedule schedule, BlockingQueue<Item> queue, CrawlIdGenerator generator,
            EventSink eventSink) {
        this.crawlSchedule = schedule;
        this.queue = queue;
        this.generator = generator;
        this.eventSink = eventSink;
    }

    @Override
    public void run() {
        doCrawl();
        isFinished = true;
    }

    private void doCrawl() {
        CrawlId currentCrawlId = generator.createNewCrawlId();
        eventSink.schedulingCrawl(currentCrawlId);
        enqueue(new Item(START_SENTINEL, currentCrawlId));
        String url;
        while (null != (url = crawlSchedule.nextUrl())) {
            Item item = new Item(url, currentCrawlId);
            enqueue(item);
        }
        //Add the end sentinel
        enqueue(new Item(END_SENTINEL, currentCrawlId));
    }

    private void enqueue(Item item) {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            eventSink.threadInterruptedError(e);
        }
    }

    @Override
    public String description() {
        return getClass().getSimpleName();
    }

}
