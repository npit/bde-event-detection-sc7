///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package gr.demokritos.iit.clustering.config;
//
//import gr.demokritos.iit.base.conf.IBaseConf;
//import gr.demokritos.iit.clustering.structs.SimilarityMode;
//
///**
// *
// * @author George K. <gkiom@iit.demokritos.gr>
// */
//public interface ISparkConf extends IClusteringConf {
//
//    String SPARK_EXECUTOR_MEMORY = "spark.executor.memory";
//    String SPARK_CASSANDRA_CONNECTION_HOST = "spark.cassandra.connection.host";
//    String SPARK_CASSANDRA_CONNECTION_PORT = "spark.cassandra.connection.port";
//    String SPARK_MASTER = "spark_master";
//    String SPARK_APP_NAME = "spark_app_name";
//    String BATCH_NUM_OF_DAYS = "batch_days_limit";
//    String NUM_PARTITIONS = "num_partitions";
//
//
//    String getMaster();
//
//    String getAppName();
//
//    String getClusterExecutorMemory();
//
//    /**
//     *
//     * @return days back to load articles batch from now
//     */
//    int getNumDaysBatch();
//
//    /**
//     * in how many partitions shall we operate upon
//     * @return
//     */
//    int getNumPartitions();
//
//    SimilarityMode getSimilarityMode();
//
//    double getCutOffThreshold();
//}
