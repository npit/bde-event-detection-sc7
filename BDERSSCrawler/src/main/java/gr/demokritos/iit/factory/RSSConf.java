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
package gr.demokritos.iit.factory;

import gr.demokritos.iit.crawlers.conf.BaseConfiguration;

/**
 * User: ade
 */
public class RSSConf extends BaseConfiguration implements IRSSConf {

    public RSSConf(String configurationFileName) {
        super(configurationFileName);
    }

    public RSSConf() {
        super();
    }

    @Override
    public int getMaxHttpConnections() {
        return Integer.parseInt(properties.getProperty("max_http_connections", "10"));
    }

    @Override
    public int getMaxQueueSize() {
        return Integer.parseInt(properties.getProperty("max_queue_size", "10"));
    }

    @Override
    public int getMaxNumberOfCrawlingThreads() {
        return Integer.parseInt(properties.getProperty("max_number_of_crawling_threads", "10"));
    }

    @Override
    public String getUrlsFileName() {
        //This property is special because if it's wrong then there's no safe default. For instance
        //ff we point the NewsCrawler at the BlogCrawler's data files then we'll corrupt our corpus.
        if (!properties.containsKey("urls_file_name")) {
            throw new IllegalArgumentException("You did not set a value for: 'urls_file_name' which would "
                    + "tell the crawler where to find its list of URLs to crawl.");
        }
        return properties.getProperty("urls_file_name");
    }

    @Override
    public int getDatabaseConnectionPoolSize() {
        return Integer.parseInt(properties.getProperty("database_connection_pool_size", "20"));
    }

    @Override
    public long getDelayBetweenCrawls() {
        String delayBetweenCrawls = properties.getProperty("delay_between_crawls", "1800");
        return Long.parseLong(delayBetweenCrawls);
    }

    @Override
    public int getHttpTimeoutInSeconds() {
        String httpTimeoutInSeconds = properties.getProperty("http_timeout_in_seconds", "60");
        return Integer.parseInt(httpTimeoutInSeconds);
    }

    @Override
    public boolean runForever() {
        return Boolean.parseBoolean(properties.getProperty("run_forever", "false"));
    }
}
