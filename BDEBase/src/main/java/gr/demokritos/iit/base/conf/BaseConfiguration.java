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
package gr.demokritos.iit.base.conf;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BaseConfiguration implements IBaseConf {

    protected final Properties properties;
    private static final String [] trueArray = {"yes","true"};
    protected static boolean isTrue(String arg)
    {
        for(String elem : trueArray)
        {
            if (elem.equals(arg.toLowerCase())) return true;
        }
        return false;
    }
    public BaseConfiguration(String configurationFileName) {
        File file = new File(configurationFileName);
        this.properties = new Properties();
        try {
            this.properties.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BaseConfiguration() {
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
    public String getSentenceSplitterModelPath() {
        return properties.getProperty("sentence_splitter_model", "./res/en-sent.bin");
    }

    @Override
    public String getStopwordsFilePath() {
        return properties.getProperty("stopwords_file_path", "./res/stopwords_en.txt");
    }
    @Override
    public String getLangDetectionProfiles()
    {
        return properties.getProperty("lang_detection_profiles", "./res/profiles");
    }
    @Override
    public String getDocumentRetrievalTimeWindow()
    {
        return properties.getProperty("retrieval_time_window","");
    }

    List<String> Modifiers;
    @Override
    public boolean hasModifier(String modifierName) {
        if(Modifiers == null)
            Modifiers = Arrays.asList(properties.getProperty("modifiers","").split(","));
        return Modifiers.contains(modifierName);
    }


}
