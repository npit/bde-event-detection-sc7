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
package gr.demokritos.iit.crawlers.load;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import gr.demokritos.iit.crawlers.schedule.DomainExtractor;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class DomainLoadRegistry implements LoadRegistry {

    private ConcurrentMap<String, Integer> hostsToLoadLevel;
    private final DomainExtractor extractor;
    private final BlockingQueue<CrawlId> conditionVariable;
    private final Set<Item> itemsBeingCrawled;
    private CrawlId currentCrawlId;

    public DomainLoadRegistry(DomainExtractor extractor) {
        this.extractor = extractor;
        this.hostsToLoadLevel = new ConcurrentHashMap<String, Integer>();
        this.conditionVariable = new LinkedBlockingQueue<CrawlId>(1);
        this.itemsBeingCrawled = Sets.newHashSet();
    }

    @Override
    public synchronized void started(Item item) {
        itemsBeingCrawled.add(item);

        String host = extractor.extractDomain(item.getFeedUrl());
        //This guarantees the map will always have at least a 0 in it
        hostsToLoadLevel.putIfAbsent(host, 0);
        if (hostsToLoadLevel.containsKey(host)) {
            Integer loadLevel = hostsToLoadLevel.get(host);
            loadLevel++;
            hostsToLoadLevel.put(host, loadLevel);
        }
    }

    @Override
    public synchronized void finished(Item item) {
        itemsBeingCrawled.remove(item);

        String host = extractor.extractDomain(item.getFeedUrl());
        Integer loadLevel = hostsToLoadLevel.get(host);
        if (loadLevel == 1) {
            //This is the last bit of load on this host so delete it from the registry
            hostsToLoadLevel.remove(host);
        } else {
            loadLevel--;
            hostsToLoadLevel.put(host, loadLevel);
        }

        if (hostsToLoadLevel.isEmpty()) {
            conditionVariable.offer(currentCrawlId);
        }
    }

    @Override
    public void startedCrawl(CrawlId crawlId) {
        this.currentCrawlId = crawlId;
    }

    @Override
    public ImmutableSet<Item> getStatus() {
        return ImmutableSet.copyOf(itemsBeingCrawled);
    }

    @Override
    public void blockUntilComplete(CrawlId crawlId) throws InterruptedException {
        //This method must not block other threads from populating the conditionVariable
        //as that is what enables this method to complete. It must also not touch any of the other member variables.

        //Block on a condition variable until there are no more crawl tasks running
        conditionVariable.take();
    }
}
