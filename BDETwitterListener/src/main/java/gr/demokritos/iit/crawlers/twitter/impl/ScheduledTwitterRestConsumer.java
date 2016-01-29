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
package gr.demokritos.iit.crawlers.twitter.impl;

import gr.demokritos.iit.crawlers.twitter.factory.Configuration;
import static gr.demokritos.iit.crawlers.twitter.factory.SystemFactory.LOGGER;
import static gr.demokritos.iit.crawlers.twitter.impl.AbstractTwitterRestConsumer.TWITTER_API_CALL_USER_TIMELINE;
import gr.demokritos.iit.crawlers.twitter.policy.ICrawlPolicy;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import gr.demokritos.iit.crawlers.twitter.structures.SearchQuery;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class ScheduledTwitterRestConsumer extends AbstractTwitterRestConsumer implements ITwitterRestConsumer, Runnable {

    private final ScheduledExecutorService executorService;

    private final long delay_between_crawls;
    private final long initial_delay;
    private final TimeUnit timeunit;

    private volatile AtomicInteger counter;
    private long engine_id;

    public ScheduledTwitterRestConsumer(Configuration config, IRepository repository, ICrawlPolicy policy) {
        super(config, repository, policy);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.delay_between_crawls = config.getDelayBetweenCrawls();
        this.initial_delay = config.getCrawlInitialDelay();
        this.timeunit = TimeUnit.MINUTES;
        this.counter = new AtomicInteger(0);
        LOGGER.info(String.format("initializing scheduled monitor execution service with initial delay: %d, delay_between_crawls: %d (%s)",
                initial_delay, delay_between_crawls, timeunit.toString().toLowerCase()));
    }

    @Override
    public void monitor() {
        engine_id = repository.scheduleInitialized(IRepository.CrawlEngine.MONITOR_FOREVER);
        ScheduledFuture<?> future;
        try {
            future = executorService.scheduleWithFixedDelay(this, 0l, delay_between_crawls, timeunit);
            future.get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.severe(ex.getMessage());
        }
    }

    @Override
    public void search(SearchQuery query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void search(Collection<SearchQuery> queries) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void run() {
        LOGGER.info(String.format("initializing scheduled monitor_forever [%d:%d] at %s", engine_id, counter.incrementAndGet(), new Date().toString()));
        Collection<SourceAccount> accounts = repository.getAccounts();
        // filter accounts to crawl
        policy.filter(accounts);
        int iCount = 1;
        int iResetCount = 1;
        int iTotal = accounts.size();
        // get rate limit status
        Map<String, Integer> checkStatus = getRateLimitStatus(TWITTER_API_CALL_USER_TIMELINE);
        long time_started = Calendar.getInstance().getTimeInMillis();
        int remaining_calls_before_limit = checkStatus.get(API_REMAINING_CALLS);
        int seconds_until_reset = checkStatus.get(API_SECONDS_UNTIL_RESET);
        if (remaining_calls_before_limit <= 0) {
            try {
                LOGGER.info(String.format("Reached Rate limit, will sleep for %d seconds to overcome", (seconds_until_reset + 1)));
                Thread.sleep(TimeUnit.MILLISECONDS.convert(seconds_until_reset + 1, TimeUnit.SECONDS));
                checkStatus = getRateLimitStatus(TWITTER_API_CALL_USER_TIMELINE);
                time_started = Calendar.getInstance().getTimeInMillis();
                remaining_calls_before_limit = checkStatus.get(API_REMAINING_CALLS);
                seconds_until_reset = checkStatus.get(API_SECONDS_UNTIL_RESET);
            } catch (InterruptedException ex) {
                LOGGER.severe(ex.getMessage());
            }
        }
        // for each account
        for (SourceAccount sourceAccount : accounts) {
            try {
                // check rate limit status
                boolean reset = checkAPICallStatus(iResetCount++, remaining_calls_before_limit, time_started, seconds_until_reset);
                if (reset) {
                    iResetCount = 1;
                    checkStatus = getRateLimitStatus(TWITTER_API_CALL_USER_TIMELINE);
                    time_started = Calendar.getInstance().getTimeInMillis();
                    remaining_calls_before_limit = checkStatus.get(API_REMAINING_CALLS);
                    seconds_until_reset = checkStatus.get(API_SECONDS_UNTIL_RESET);
                }
                String sourceName = sourceAccount.getAccount();
                LOGGER.info(String.format("Parsing '%s': %d/%d accounts", sourceName, iCount++, iTotal));
                // get posts from selected account
                List<Status> statuses = twitter.getUserTimeline(sourceName);
                // process statuses
                List<Status> res = processStatuses(statuses, IRepository.CrawlEngine.MONITOR_FOREVER, engine_id);
                // log done
                LOGGER.info(String.format("Finished: '%s' with %d updates", sourceName, res.size()));
            } catch (TwitterException ex) {
                LOGGER.severe(ex.getMessage());
            } catch (InterruptedException ex) {
                LOGGER.severe(ex.getMessage());
            }
        }
        LOGGER.info(String.format("finalized scheduled monitor_forever [%d:%d] at %s, next schedule in %d %s",
                engine_id, counter.get(), new Date().toString(), delay_between_crawls, timeunit.toString()));
    }
}
