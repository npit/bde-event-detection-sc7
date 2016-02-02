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
package gr.demokritos.iit.crawlers;

import gr.demokritos.iit.runnable.LoggingRunnableDecorator;
import gr.demokritos.iit.runnable.Consumer;
import gr.demokritos.iit.crawlers.event.EventSink;
import gr.demokritos.iit.crawlers.load.DomainLoadRegistry;
import gr.demokritos.iit.crawlers.load.LoadRegistry;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.crawlers.schedule.CrawlIdGenerator;
import gr.demokritos.iit.crawlers.schedule.CrawlSchedulerTask;
import gr.demokritos.iit.crawlers.schedule.CrawlExecutor;
import gr.demokritos.iit.crawlers.schedule.DefaultScheduleLoader;
import gr.demokritos.iit.crawlers.schedule.DefaultCrawlExecutor;
import gr.demokritos.iit.crawlers.schedule.DefaultCrawlIdGenerator;
import gr.demokritos.iit.factory.SystemFactory;
import gr.demokritos.iit.factory.RSSConf;
import gr.demokritos.iit.crawlers.schedule.DomainExtractor;
import gr.demokritos.iit.crawlers.schedule.DefaultCrawlSchedule;
import gr.demokritos.iit.crawlers.schedule.CrawlSchedule;
import gr.demokritos.iit.factory.IRSSConf;
import gr.demokritos.iit.repository.IRepository;
import org.apache.http.client.HttpClient;

import java.util.concurrent.*;

/**
 *
 */
public abstract class AbstractCrawler {


    private final Consumer consumer;
    protected ScheduledExecutorService producerExecutorService;
    protected ExecutorService oneTimeProducerExecutorService;
    private final ExecutorService consumerExecutorService;
    private final EventSink eventSink;
    private final CrawlSchedule schedule;
    protected final BlockingQueue<Item> queue;
    protected final CrawlSchedulerTask crawlSchedulerTask;
    private long delayBetweenCrawls;
    // run forever or for a single time 
    protected boolean bRunForever;

    public AbstractCrawler(SystemFactory factory, IRSSConf configuration,
            boolean shouldApplyRobotsExclusionRules) throws Exception {
        eventSink = factory.createEventSink();
        queue = factory.createBlockingQueue();
        IRepository repository = createRepository(factory);

        // Set up the consumer and the crawl executor
        HttpClient httpClient = factory.createHttpClient();
        consumerExecutorService = factory.createConsumerExecutorService();
        LoadRegistry loadRegistry = new DomainLoadRegistry(new DomainExtractor());
        CrawlExecutor crawlExecutor = new DefaultCrawlExecutor(consumerExecutorService, repository, httpClient,
                loadRegistry, eventSink, shouldApplyRobotsExclusionRules);
        consumer = new Consumer(queue, crawlExecutor, loadRegistry, repository, eventSink, bRunForever);

        // disabled status server: 21/05/14
//        //set up the status server
//        StatusServer statusServer = new StatusServer(loadRegistry, configuration);
//        statusServer.runInBackground();
        // set up the schedule
        DefaultScheduleLoader loader = factory.createScheduleLoader(eventSink);
        schedule = new DefaultCrawlSchedule(loader, new DomainExtractor());
        // run forever?
        bRunForever = configuration.runForever();
        if (bRunForever) {
            // Only 1 crawl scheduling thread is necessary. It will repeatedly invoke this one instance of the
            // crawl scheduling task
            this.producerExecutorService = factory.createProducerExecutorService();
            delayBetweenCrawls = configuration.getDelayBetweenCrawls();
        } else {
            // create a simple single thread executor
            this.oneTimeProducerExecutorService = factory.createOneTimeProducerExecutorService();
        }
        CrawlIdGenerator generator = new DefaultCrawlIdGenerator(repository);
        crawlSchedulerTask = new CrawlSchedulerTask(schedule, queue, generator, eventSink);
        // At this point, everything that can be configured has been configured and all we need to do is start crawling
    }

    protected abstract IRepository createRepository(SystemFactory factory);

    public void startCrawling() throws ExecutionException, InterruptedException {
        // Start the consumer in a different thread. That thread will block until there are items on the queue
        // The consumer gets started and it just runs on its own
        consumerExecutorService.execute(new LoggingRunnableDecorator(consumer, eventSink));
        // if we want to crawl forever 
        if (bRunForever) {
            runForever();
        } else {
            runOnce();
        }
    }

    protected void runForever() throws InterruptedException, ExecutionException {
        // Set up the producer.
        ScheduledFuture<?> scheduledFuture;
        long initialDelay = 0;
        // This blocks the main thread forever or until something goes wrong with scheduling a crawl.
        scheduledFuture = producerExecutorService.scheduleWithFixedDelay(crawlSchedulerTask,
                initialDelay, delayBetweenCrawls, TimeUnit.SECONDS);
        // start the producer
        scheduledFuture.get();
    }

    private void runOnce() throws InterruptedException, ExecutionException {
        // Set up the task
        oneTimeProducerExecutorService.submit(crawlSchedulerTask);
        
        // wait until crawl schedule is finished
        while (!crawlSchedulerTask.isFinished) {
            Thread.sleep(10000);
        }
        // shutdown producer when completed
        oneTimeProducerExecutorService.shutdown();
        try {
            oneTimeProducerExecutorService.awaitTermination(6, TimeUnit.HOURS);
            eventSink.shutdown("producer");
        } catch (InterruptedException ex) {
            eventSink.error(ex);
        }
        // shutdown consumer when completed
        consumerExecutorService.shutdown();
        try {
            consumerExecutorService.awaitTermination(6, TimeUnit.HOURS);
            eventSink.shutdown("consumer");
        } catch (InterruptedException ex) {
            eventSink.error(ex);
        }
    }

    public static final String DEFAULT_NEWS_CONFIGURATION = "res/newscrawler_configuration.properties";
    public static final String DEFAULT_BLOG_CONFIGURATION = "res/blogcrawler_configuration.properties";

}
