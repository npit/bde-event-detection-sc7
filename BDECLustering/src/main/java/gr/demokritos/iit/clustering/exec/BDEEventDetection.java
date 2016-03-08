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
package gr.demokritos.iit.clustering.exec;

import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;
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

        // load batch. The tuple4 shall be <entry_url, title, clean_text, timestamp>
        JavaRDD<Tuple4<String, String, String, Long>> RDDbatch = repo.loadArticlesPublishedLaterThan(timestamp);

        JavaPairRDD<
        Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs
                = combinations(RDDbatch);
        // implement algorithm from NewSum.
        // create pairs from original articles
        // get matching mappings
        // generate clusters

    }

    public static JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> combinations(JavaRDD<Tuple4<String, String, String, Long>> original) {
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
        return original.cartesian(original).filter(new DocumentPairGenerationFilterFunction());
    }

}
