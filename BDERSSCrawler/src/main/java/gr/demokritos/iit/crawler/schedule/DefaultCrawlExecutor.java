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
package gr.demokritos.iit.crawler.schedule;

import gr.demokritos.iit.crawler.Fetcher;
import gr.demokritos.iit.crawler.HttpFetcher;
import gr.demokritos.iit.crawler.LoadTrackingDecorator;
import gr.demokritos.iit.crawler.event.EventSink;
import gr.demokritos.iit.crawler.load.LoadRegistry;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.repository.IRepository;
import gr.demokritos.iit.runnable.FeedFetchTask;
import gr.demokritos.iit.runnable.LoggingRunnableDecorator;
import java.util.concurrent.ExecutorService;
import org.apache.http.client.HttpClient;

public class DefaultCrawlExecutor implements CrawlExecutor {

    private final ExecutorService executorService;
    private final IRepository repository;
    private final HttpClient client;
    private final LoadRegistry loadRegistry;
    private final EventSink eventSink;
    private final boolean shouldApplyRobotsExclusionRules;

    public DefaultCrawlExecutor(ExecutorService executorService, IRepository repository, HttpClient client,
            LoadRegistry loadRegistry, EventSink eventSink, boolean shouldApplyRobotsExclusionRules) {
        this.executorService = executorService;
        this.repository = repository;
        this.client = client;
        this.loadRegistry = loadRegistry;
        this.eventSink = eventSink;
        this.shouldApplyRobotsExclusionRules = shouldApplyRobotsExclusionRules;
    }

    @Override
    public void executeWith(Item item) {
        executorService.execute(newCrawlTask(item));
    }

    private Runnable newCrawlTask(Item item) {
        // There should be one HttpFetcher per crawl task invocation.
        Fetcher fetcher = new HttpFetcher(client, repository, shouldApplyRobotsExclusionRules);
        // TODO: CHECK if should be some other type of task
        FeedFetchTask feedFetchTask = new FeedFetchTask(item, fetcher, repository, eventSink);
        return new LoadTrackingDecorator(item, loadRegistry, new LoggingRunnableDecorator(feedFetchTask, eventSink));
    }
}
