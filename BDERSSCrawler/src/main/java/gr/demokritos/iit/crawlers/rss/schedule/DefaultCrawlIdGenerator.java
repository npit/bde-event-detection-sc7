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
package gr.demokritos.iit.crawlers.rss.schedule;

import gr.demokritos.iit.crawlers.rss.model.CrawlId;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;

public class DefaultCrawlIdGenerator implements CrawlIdGenerator {

    private final IRepository repository;

    public DefaultCrawlIdGenerator(IRepository repository) {
        this.repository = repository;
    }

    @Override
    public CrawlId createNewCrawlId() {
        CrawlId currentCrawlId = getCurrentCrawlId();
        CrawlId newCrawlId = createNextCrawlId(currentCrawlId);
        repository.saveCrawlId(newCrawlId);
        return newCrawlId;
    }

    private CrawlId createNextCrawlId(CrawlId currentCrawlId) {
        if (currentCrawlId == null) {
            return new CrawlId(1);
        }
        CrawlId newCrawlId = new CrawlId(currentCrawlId.getId() + 1);
        return newCrawlId;
    }

    @Override
    public CrawlId getCurrentCrawlId() {
        return repository.findMostRecentCrawlId();
    }
}
