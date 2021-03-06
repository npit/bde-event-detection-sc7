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
package gr.demokritos.iit.crawlers.twitter.policy;

import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import java.util.Collection;

/**
 * do not filter any account, keep all.
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class AggressiveCrawlPolicy extends AbstractCrawlPolicy implements ICrawlPolicy {

    public AggressiveCrawlPolicy(IRepository repository) {
        super(repository);
    }

    @Override
    public void filter(Collection<SourceAccount> accounts) {
    }
}
