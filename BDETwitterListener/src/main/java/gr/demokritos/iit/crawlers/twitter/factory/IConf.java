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

import gr.demokritos.iit.crawlers.twitter.impl.ITwitterRestConsumer;
import gr.demokritos.iit.crawlers.twitter.impl.ScheduledTwitterRestConsumer;
import gr.demokritos.iit.crawlers.twitter.policy.InfluentialCrawlPolicy;
import org.apache.http.client.methods.HttpGet;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IConf {

    String FILE_SEPARATOR = System.getProperty("file.separator");
    String UTF8 = "UTF-8";

    int getCacheSize();

    String getCassandraClusterName();

    /**
     *
     * @return the keyspace to use (Cassandra backend only)
     */
    String[] getCassandraHosts();

    /**
     *
     * @return the keyspace to use (Cassandra backend only)
     */
    String getCassandraKeyspace();

    /**
     *
     * @return the cassandra listen port
     */
    int getCassandraPort();

    /**
     * used by the {@link HttpGet} implementation for shutting down inactive
     * hosts
     *
     * @return
     */
    int getConnectionTimeOut();

    int getSocketTimeout();

    /**
     * Used by the {@link ScheduledTwitterRestConsumer} implementation in
     * minutes
     *
     * @return
     */
    int getCrawlInitialDelay();

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

    int getDataSourceAcquireIncrement();

    int getDataSourceMaxPoolSize();

    int getDataSourceMaxStatements();

    int getDataSourceMinPoolSize();

    String getDatabaseHost();

    /**
     * MySQL backend
     *
     * @return the database name that we write into
     */
    String getDatabaseName();

    /**
     * MySQL backend
     *
     * @return
     */
    String getDatabasePassword();

    /**
     * MySQL backend
     *
     * @return
     */
    String getDatabaseUserName();

    /**
     * Used by the {@link ScheduledTwitterRestConsumer} implementation in
     * minutes
     *
     * @return
     */
    int getDelayBetweenCrawls();

    /**
     *
     * @return the required followers a user must have in order to be crawled,
     * if {@link InfluentialCrawlPolicy} implementation is used, else ignored
     */
    int getFollowersCutOff();

    String getGeoNamesClientUserName();

    /**
     * the full classname for the repository implementation to utilize
     *
     * @return
     */
    String getRepositoryImpl();

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

    /**
     *
     * @return the directory where all files are stored and read from
     */
    String getWorkingDir();

    /**
     * Manually sets 'working directory'
     *
     * @param sWorkingDir the path to set
     * @see {@link #getWorkingDir() }
     */
    void setWorkingDir(String sWorkingDir);

}
