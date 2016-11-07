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

import gr.demokritos.iit.crawlers.twitter.structures.SearchQuery;
import java.util.Collection;

/**
 * consumer twitter Rest API.
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ITwitterRestConsumer {

    /**
     * monitor the user accounts provided in the DB
     */
    public void monitor();

    /**
     * search using twitter API
     *
     * @param query
     */
    public void search(SearchQuery query);

    /**
     * search iteratively using twitter API
     *
     * @param queries
     */
    public void search(Collection<SearchQuery> queries);

    void fetch(Collection<Long> tweetID);
}
