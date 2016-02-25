/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.exec;

import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDEEventDetection {

//    private final SparkContext sc;
    private final BDESpark sp;

    public BDEEventDetection(BDESpark bdes) {
        this.sp = bdes;
    }

    public SparkContext getContext() {
        return sp.getContext();
    }

    public static void main(String[] args) {
//        ISparkConf conf = new BDESparkConf("spark.properties");
        ISparkConf conf = new BDESparkConf();
        BDESpark bdes = new BDESpark(conf);

        BDEEventDetection bdedet = new BDEEventDetection(bdes);

        SparkContext sc = bdedet.getContext();

        CassandraSparkRepository repo = new CassandraSparkRepository(sc, conf.getCassandraKeyspace());

        long timestamp = repo.getLatestTimestamp("event_detection_log"); // TODO: add table.
        System.out.println(new Date(timestamp).toString());

        JavaRDD<Tuple4<String, String, String, Long>> RDDbatch = repo.loadArticlesPublishedLaterThan(timestamp);

//        List<Tuple3<String, String, Long>> collect = RDDbatch.collect();
//        for (Tuple3<String, String, Long> collect1 : collect) {
//            System.out.println(collect1._1());
//            System.out.println(collect1._3());
//        }
        
        // implement algorithm from NewSum.
        
        // create pairs from original articles
        
        // get matching mappings
        
        // generate clusters
        
        
        
        final List<Tuple2<Tuple2<String, String>, Tuple2<String, String>>> pairs = new ArrayList();


    }

}
