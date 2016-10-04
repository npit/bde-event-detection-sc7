/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.clustering.structs.SimilarityMode;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDESparkConf extends clusteringConf implements ISparkConf {

    public BDESparkConf(String configurationFileName) {
        super(configurationFileName);
    }

    /**
     * uses some default properties for local dev.
     * Empty the properties map first and assign your own later
     */
    public BDESparkConf() {
        super();
        properties.setProperty(SPARK_MASTER, "local[2]");
        properties.setProperty(SPARK_APP_NAME, "EventDetection");
        properties.setProperty(SPARK_EXECUTOR_MEMORY, "1g");
        properties.setProperty("cassandra_hosts", "127.0.0.1");
        properties.setProperty("cassandra_port", "9042");
        properties.setProperty("cassandra_keyspace", "bde");
        properties.setProperty("cassandra_cluster_name", "Test Cluster");
    }


    @Override
    public String getMaster() {
        return properties.getProperty(SPARK_MASTER, "local[4]");
    }

    @Override
    public String getAppName() {
        return properties.getProperty(SPARK_APP_NAME, "EventDetection");
    }

    @Override
    public String getClusterExecutorMemory() {
        return properties.getProperty(SPARK_EXECUTOR_MEMORY, "1g");
    }

    @Override
    public int getNumDaysBatch() {
        return Integer.valueOf(properties.getProperty(BATCH_NUM_OF_DAYS, "2"));
    }

    @Override
    public int getNumPartitions() {
        return Integer.valueOf(properties.getProperty(NUM_PARTITIONS, "4"));
    }

    @Override
    public SimilarityMode getSimilarityMode() {
        try {
            SimilarityMode mode = SimilarityMode.valueOf(properties.getProperty(SIMILARITY_MODE_PARAM, "nvs").toUpperCase());
            return mode;
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println("fallback to default similarity mode: NVS");
            return SimilarityMode.NVS;
        }
    }

    @Override
    public double getCutOffThreshold() {
        return Double.valueOf(properties.getProperty(CUTOFF_THRESHOLD_PARAM, "0.24"));
    }
}
