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

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.crawlers.twitter.policy.InfluentialCrawlPolicy;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class TConfig extends BaseConfiguration implements ITwitterConf {

    public TConfig(String configurationFileName) {
        super(configurationFileName);
    }

    private TConfig() {
        super();
    }

    @Override
    public String getTwitterConsumerKey() {
        return properties.getProperty("twitterConsumerKey");
    }

    @Override
    public String getTwitterConsumerKeySecret() {
        return properties.getProperty("twitterConsumerKeySecret");
    }

    @Override
    public String getTwitterAccessTokken() {
        return properties.getProperty("twitterAccessTokken");
    }

    @Override
    public String getTwitterAccessTokkenSecret() {
        return properties.getProperty("twitterAccessTokkenSecret");
    }

    @Override
    public String getCrawlPolicy() {
        return properties.getProperty("crawl_policy");
    }

    @Override
    public String getCrawlerImpl() {
        return properties.getProperty("crawl_impl");
    }

    @Override
    public String getURLUnshortenerImpl() {
        return properties.getProperty("unshorthener_impl");
    }

    @Override
    public String getGeoNamesClientUserName() {
        return properties.getProperty("geonames_client_name");
    }

    /**
     *
     * @return the required followers a user must have in order to be crawled,
     * if {@link InfluentialCrawlPolicy} implementation is used, else ignored
     */
    @Override
    public int getFollowersCutOff() {
        return Integer.parseInt(properties.getProperty("followers_count_cutoff", "100"));
    }

    @Override
    public String getStreamImpl() {
        return properties.getProperty("stream_impl");
    }

    @Override
    public String getStreamLanguage() {
        return properties.getProperty("stream_lang", "en");
    }

    @Override
    public int getDelayBetweenCrawls() {
        String delayBetweenCrawls = properties.getProperty("delay_between_crawls");
        return Integer.parseInt(delayBetweenCrawls);
    }

    @Override
    public int getCrawlInitialDelay() {
        String initdel = properties.getProperty("initial_delay");
        return Integer.parseInt(initdel);
    }
}
