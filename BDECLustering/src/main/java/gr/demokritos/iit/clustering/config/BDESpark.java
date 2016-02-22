/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.config;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDESpark {

    private final SparkContext ctx;

    public BDESpark(ISparkConf config) {
        SparkConf conf = new SparkConf(true)
                .setMaster(config.getMaster())
                .setAppName(config.getAppName())
                .set(ISparkConf.SPARK_EXECUTOR_MEMORY, config.getClusterExecutorMemory())
                .set(ISparkConf.SPARK_CASSANDRA_CONNECTION_HOST, config.getCassandraHosts()[0]) // TODO FIXME
                .set(ISparkConf.SPARK_CASSANDRA_CONNECTION_PORT, String.valueOf(config.getCassandraPort()));
        this.ctx = new SparkContext(conf);
    }

    public SparkContext getContext() {
        return ctx;
    }
}
