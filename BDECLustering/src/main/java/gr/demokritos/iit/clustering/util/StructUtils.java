package gr.demokritos.iit.clustering.util;

import org.apache.spark.api.java.JavaPairRDD;
import scala.Tuple2;
import scala.Tuple4;

import java.util.List;

public class StructUtils {

    public static void printArticlePairs(JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> rddPairs, int items ) {

        System.out.println("PAIRS:\n");
        List<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>> takeN = rddPairs.take(items);

        for (Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> eachPair: takeN) {
            System.out.println("url1: " + eachPair._1._1() + ", url2: " + eachPair._2._1());
            System.out.println("title1: " + eachPair._1._2() + ", title2: " + eachPair._2._2());
        }
    }
}
