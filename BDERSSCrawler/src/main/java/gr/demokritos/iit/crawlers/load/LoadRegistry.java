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

import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.model.CrawlId;
import com.google.common.collect.ImmutableSet;

/**
 * User: ade
 */
public interface LoadRegistry {

    void blockUntilComplete(CrawlId crawlId) throws InterruptedException;

    void started(Item item);

    void finished(Item item);

    void startedCrawl(CrawlId crawlId);

    /**
     * An immutable copy of the set of items that are currently being crawled
     *
     * @return
     */
    ImmutableSet<Item> getStatus();
}
