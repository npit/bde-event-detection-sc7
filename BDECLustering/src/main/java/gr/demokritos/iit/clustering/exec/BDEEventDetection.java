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
import org.apache.spark.Partition;
import org.apache.spark.SparkContext;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.rdd.RDD;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;
import scala.collection.Iterator;
import scala.collection.Seq;

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

    public JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> combinations(JavaRDD<Tuple4<String, String, String, Long>> original) {
//        // example scala code.
//        //        def combs(rdd:RDD[String]):RDD[(String,String)] = {
//        //    val count = rdd.count
//        //    if (rdd.count < 2) { 
//        //        sc.makeRDD[(String,String)](Seq.empty)
//        //    } else if (rdd.count == 2) {
//        //        val values = rdd.collect
//        //        sc.makeRDD[(String,String)](Seq((values(0), values(1))))
//        //    } else {
//        //        val elem = rdd.take(1)
//        //        val elemRdd = sc.makeRDD(elem)
//        //        val subtracted = rdd.subtract(elemRdd)  
//        //        val comb = subtracted.map(e  => (elem(0),e))
//        //        comb.union(combs(subtracted))
//        //    } 
//        // }
//        JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> res;
//        SparkContext sc = getContext();
//        long items = original.count();
//        if (items < 2) {
//            return null;
//        } else if (items == 2l) {
//            res = new JavaPairRDD()
//        }
        // simpler (more expensive?)
        // rdd.cartesian(rdd).filter{ case (a,b) => a < b }`.

        original.cartesian(original).filter(new Function<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>, Boolean>() {

            @Override
            public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> v1) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        
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
