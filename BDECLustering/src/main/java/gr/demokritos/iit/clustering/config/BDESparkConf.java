/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.BaseConfiguration;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDESparkConf extends BaseConfiguration implements ISparkConf {

    public BDESparkConf(String configurationFileName) {
        super(configurationFileName);
    }

    public BDESparkConf() {
        super();
        properties.setProperty("master", "local[2]");
        properties.setProperty("app_name", "EventDetection");
        properties.setProperty(SPARK_EXECUTOR_MEMORY, "1g");
        properties.setProperty("cassandra_hosts", "127.0.0.1");
        properties.setProperty("cassandra_port", "9042");
        properties.setProperty("cassandra_keyspace", "bde");
    }

    @Override
    public String getMaster() {
        return "local[4]";
    }

    @Override
    public String getAppName() {
        return "EventDetection";
    }

    @Override
    public String getClusterExecutorMemory() {
        return properties.getProperty(SPARK_EXECUTOR_MEMORY, "1g");
    }

}
