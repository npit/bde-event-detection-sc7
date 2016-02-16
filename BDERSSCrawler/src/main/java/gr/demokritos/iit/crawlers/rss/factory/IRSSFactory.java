/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.crawlers.rss.factory;

import gr.demokritos.iit.crawlers.rss.AbstractCrawler;
import gr.demokritos.iit.crawlers.rss.event.EventSink;
import gr.demokritos.iit.crawlers.rss.schedule.DefaultScheduleLoader;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;
import java.beans.PropertyVetoException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import org.apache.http.client.HttpClient;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IRSSFactory {

    Logger log = Logger.getLogger(AbstractCrawler.class.getName());

    BlockingQueue<Item> createBlockingQueue();

    IRepository createBlogRepository() throws PropertyVetoException;

    ExecutorService createConsumerExecutorService();

    EventSink createEventSink();

    HttpClient createHttpClient();

    IRepository createNewsRepository() throws PropertyVetoException;

    ExecutorService createOneTimeProducerExecutorService();

    ScheduledExecutorService createProducerExecutorService();

    DefaultScheduleLoader createScheduleLoader(EventSink eventSink);

    /**
     * release underlying DB connection pools
     */
    void releaseResources();

}
