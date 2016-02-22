/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.IBaseConf;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ISparkConf extends IBaseConf {

    String SPARK_EXECUTOR_MEMORY = "spark.executor.memory";
    String SPARK_CASSANDRA_CONNECTION_HOST = "spark.cassandra.connection.host";
    String SPARK_CASSANDRA_CONNECTION_PORT = "spark.cassandra.connection.port";

    String getMaster();

    String getAppName();

    String getClusterExecutorMemory();
}
