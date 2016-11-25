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
package gr.demokritos.iit.crawlers.rss;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.crawlers.rss.extractors.DateExtractor;
import gr.demokritos.iit.crawlers.rss.model.Content;
import gr.demokritos.iit.crawlers.rss.model.CrawlId;
import gr.demokritos.iit.crawlers.rss.runnable.FeedFetchTask;
import gr.demokritos.iit.crawlers.rss.runnable.LoggingRunnableDecorator;
import gr.demokritos.iit.crawlers.rss.runnable.Consumer;
import gr.demokritos.iit.crawlers.rss.event.EventSink;
import gr.demokritos.iit.crawlers.rss.load.DomainLoadRegistry;
import gr.demokritos.iit.crawlers.rss.load.LoadRegistry;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlIdGenerator;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlSchedulerTask;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlExecutor;
import gr.demokritos.iit.crawlers.rss.schedule.DefaultScheduleLoader;
import gr.demokritos.iit.crawlers.rss.schedule.DefaultCrawlExecutor;
import gr.demokritos.iit.crawlers.rss.schedule.DefaultCrawlIdGenerator;
import gr.demokritos.iit.crawlers.rss.factory.RSSCrawlFactory;
import gr.demokritos.iit.crawlers.rss.schedule.DomainExtractor;
import gr.demokritos.iit.crawlers.rss.schedule.DefaultCrawlSchedule;
import gr.demokritos.iit.crawlers.rss.schedule.CrawlSchedule;
import gr.demokritos.iit.crawlers.rss.factory.conf.IRSSConf;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
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
    HttpClient httpClient;
    IRepository repository;

    public AbstractCrawler(RSSCrawlFactory factory, IRSSConf configuration) throws Exception {

        eventSink = factory.createEventSink();
        queue = factory.createBlockingQueue();
        repository = createRepository(factory);
        repository.setCrawlMode(configuration.getOperationMode());
        // Set up the consumer and the crawl executor
        httpClient = factory.createHttpClient();
        consumerExecutorService = factory.createConsumerExecutorService();
        LoadRegistry loadRegistry = new DomainLoadRegistry(new DomainExtractor());
        CrawlExecutor crawlExecutor = new DefaultCrawlExecutor(consumerExecutorService, repository, httpClient,
                loadRegistry, eventSink, configuration.getRespectRobots(),configuration.applyHTTPFetchRestrictions());
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

    protected abstract IRepository createRepository(RSSCrawlFactory factory);

    public void fetch(IRSSConf configuration) {
        Fetcher fetcher = new HttpFetcher(httpClient, repository, configuration.getRespectRobots(),
                configuration.applyHTTPFetchRestrictions());

        String filename = configuration.getUrlsFileName();
        File urlsFile = new File(filename);
        DefaultCrawlIdGenerator idgen = new DefaultCrawlIdGenerator(repository);
        CrawlId CrawlID = idgen.createNewCrawlId();

        ArrayList<String> urls = new ArrayList<>();
        try {
            for (String line : Files.readLines(urlsFile, Charsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue; // comments
                urls.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean retry = false;
        for (int s =0; s< urls.size(); s++) {
            String url =  urls.get(s);
            try {
                Document doc = null;
                System.out.printf("Fetching url %d/%d : [%s]\n", 1+s,urls.size(),url);
                try {
                    doc = Jsoup.connect(url).get();
                }
                catch (org.jsoup.HttpStatusException e) {
                    //e.printStackTrace();
                    System.err.println("\tJsoup http status error. Continuing...");
                    continue;
                }

                if (doc == null) continue;
                String title = doc.title();

                Date articleDate = DateExtractor.getDate(doc, url);

                Item item = new Item("no-feed", CrawlID);

                Content htmlContent = fetcher.fetchUrl(url);
                if(htmlContent == null)
                {
                    if(retry == false) {
                        System.out.println("\tNull html content, retrying");
                        --s;
                        retry = true;
                    }
                    continue;
                }
                else
                    retry = false;

                if (title == null) {
                    title = "";
                } else {
                    title = StringUtils.normalizeSpace(title.replaceAll("\\x7f", " "));
                }

                repository.savePage(item, title, htmlContent, articleDate);

            }

             catch(IOException e){
                e.printStackTrace();
            } catch(BoilerpipeProcessingException e){
                e.printStackTrace();
            }
    }
        CrawlID.finishedCrawling();
        repository.saveCrawlId(CrawlID);
    }
    public void startCrawling() throws ExecutionException, InterruptedException {
        // Start the consumer in a different thread. That thread will block until there are items on the queue
        // The consumer gets started and it just runs on its own
        try {
            consumerExecutorService.execute(new LoggingRunnableDecorator(consumer, eventSink));
            // if we want to crawl forever
            if (bRunForever) {
                runForever();
            } else {
                runOnce();
            }
        }catch(RuntimeException ex)
        {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    protected void runForever() throws InterruptedException, ExecutionException {
        System.out.println("Running runForever()... ");
        // Set up the producer.
        ScheduledFuture<?> scheduledFuture;
        long initialDelay = 0;
        // This blocks the main thread forever or until something goes wrong with scheduling a crawl.
        scheduledFuture = producerExecutorService.scheduleWithFixedDelay(crawlSchedulerTask,
                initialDelay, delayBetweenCrawls, TimeUnit.SECONDS);
        // start the producer
        scheduledFuture.get();
    }

    protected void runOnce() throws InterruptedException, ExecutionException {
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
