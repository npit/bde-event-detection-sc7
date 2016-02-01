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
package gr.demokritos.iit.crawlers.event;

import gr.demokritos.iit.model.Content;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;

public interface EventSink {

    void error(Exception e);

    void error(String message, Exception e);

    /**
     * If this happens it is important not to swallow the exception but to
     * restore the current thread's interrupted status
     *
     * @param e
     */
    void threadInterruptedError(InterruptedException e);

    void error(String message);

    //The numbers in the comments attached to the following methods indicate the order in which these events are
    //usually expected to happen.
    void loadingSchedule();//1

    void schedulingCrawl(CrawlId crawlId);//2

    void startingToConsumeScheduledUrls();//3

    void startedCrawling(CrawlId crawlId);//4

    void fetching(Item item);//5

    void finishedFetching(Item item, Content contentFromFeed);//6

    void prepareToFinishCrawling(CrawlId crawlId);//7

    void finishedCrawling(CrawlId crawlId);//8

    void shutdown(String sTask);

}
