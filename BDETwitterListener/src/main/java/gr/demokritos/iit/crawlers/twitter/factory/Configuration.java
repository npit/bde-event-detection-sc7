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

import gr.demokritos.iit.crawlers.twitter.policy.InfluentialCrawlPolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class Configuration implements IConf {

    private final Properties properties;

    public Configuration(String configurationFileName) {
        File file = new File(configurationFileName);
        this.properties = new Properties();
        try {
            this.properties.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Configuration() {
        this.properties = new Properties();
    }

    @Override
    public String getDatabaseHost() {
        return properties.getProperty("databaseHost");
    }

    @Override
    public String getDatabaseUserName() {
        return properties.getProperty("databaseUsername");
    }

    @Override
    public String getDatabasePassword() {
        return properties.getProperty("databasePassword");
    }

    @Override
    public String getRepositoryImpl() {
        return properties.getProperty("repository_impl");
    }

    /**
     *
     * @return the keyspace to use (Cassandra backend only)
     */
    @Override
    public String getCassandraKeyspace() {
        return properties.getProperty("cassandra_keyspace").trim();
    }

    /**
     *
     * @return the keyspace to use (Cassandra backend only)
     */
    @Override
    public String[] getCassandraHosts() {
        String hosts = properties.getProperty("cassandra_hosts");
        return hosts.split(", ");
    }

    @Override
    public String getCassandraClusterName() {
        return properties.getProperty("cassandra_cluster_name", "Test Cluster");
    }

    @Override
    public int getCassandraPort() {
        return Integer.parseInt(properties.getProperty("cassandra_port", "9042"));
    }

    /**
     *
     * @return the database name that we write into
     */
    @Override
    public String getDatabaseName() {
        return properties.getProperty("databasename");
    }

    @Override
    public int getConnectionTimeOut() {
        return Integer.parseInt(properties.getProperty("connection_timeout"));
    }

    @Override
    public int getSocketTimeout() {
        return Integer.parseInt(properties.getProperty("socket_timeout"));
    }

    @Override
    public int getCacheSize() {
        return Integer.parseInt(properties.getProperty("cache_size"));
    }

    /**
     *
     * @return the directory where all files are stored and read from
     */
    @Override
    public String getWorkingDir() {
        String sWorkingDir = properties.getProperty("workingDir");

        if (!sWorkingDir.endsWith(FILE_SEPARATOR)) {
            return sWorkingDir + FILE_SEPARATOR;
        } else {
            return sWorkingDir;
        }

    }

    /**
     * Manually sets 'working directory'
     *
     * @param sWorkingDir the path to set
     * @see {@link #getWorkingDir() }
     */
    @Override
    public void setWorkingDir(String sWorkingDir) {
        properties.put("workingDir", sWorkingDir);
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
    public int getDataSourceMinPoolSize() {
        return Integer.parseInt(properties.getProperty("min_pool_size", "5"));
    }

    @Override
    public int getDataSourceAcquireIncrement() {
        return Integer.parseInt(properties.getProperty("acquire_increment", "5"));
    }

    @Override
    public int getDataSourceMaxPoolSize() {
        return Integer.parseInt(properties.getProperty("max_pool_size", "20"));
    }

    @Override
    public int getDataSourceMaxStatements() {
        return Integer.parseInt(properties.getProperty("max_statements", "180"));
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
     * in minutes
     *
     * @return
     */
    @Override
    public int getDelayBetweenCrawls() {
        return Integer.parseInt(properties.getProperty("delay_between_crawls", "1"));
    }

    /**
     * in minutes
     *
     * @return
     */
    @Override
    public int getCrawlInitialDelay() {
        return Integer.parseInt(properties.getProperty("initial_delay", "1"));
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

}
