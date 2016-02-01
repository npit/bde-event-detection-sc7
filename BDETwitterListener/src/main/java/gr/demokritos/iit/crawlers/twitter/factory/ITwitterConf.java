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
package gr.demokritos.iit.crawlers.twitter.factory;

import gr.demokritos.iit.crawlers.conf.IBaseConf;
import gr.demokritos.iit.crawlers.twitter.impl.ITwitterRestConsumer;
import gr.demokritos.iit.crawlers.twitter.policy.InfluentialCrawlPolicy;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ITwitterConf extends IBaseConf {

    /**
     * the crawl policy to utilize. Used by all implementations of
     * {@link ITwitterRestConsumer}
     *
     * @return
     */
    String getCrawlPolicy();

    /**
     * the (rest API) crawler impl to use
     *
     * @return
     */
    String getCrawlerImpl();

    /**
     *
     * @return the required followers a user must have in order to be crawled,
     * if {@link InfluentialCrawlPolicy} implementation is used, else ignored
     */
    int getFollowersCutOff();

    String getGeoNamesClientUserName();

    /**
     * the full classname for the stream implementation to utilize
     *
     * @return
     */
    String getStreamImpl();

    String getTwitterAccessTokken();

    String getTwitterAccessTokkenSecret();

    String getTwitterConsumerKey();

    String getTwitterConsumerKeySecret();

    String getURLUnshortenerImpl();

    int getDelayBetweenCrawls();

    int getCrawlInitialDelay();
}
