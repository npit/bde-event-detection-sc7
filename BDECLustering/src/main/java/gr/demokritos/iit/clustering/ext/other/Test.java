/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.ext.other;

import java.util.Arrays;
import java.util.List;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class Test {

    public static void main(String[] args) {
        String appName = "test";
        String master = "local";
        SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
        JavaSparkContext sc = new JavaSparkContext(conf);

        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaRDD<Integer> distData = sc.parallelize(data);
        class GetLength implements Function<String, Integer> {

            @Override
            public Integer call(String s) {
                return s.length();
            }
        }

        class Sum implements Function2<Integer, Integer, Integer> {

            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        }
        System.out.println(distData.reduce(new Sum()));
        JavaRDD<String> lines = sc.textFile("data.txt");
        JavaRDD<Integer> lineLengths = lines.map(new GetLength());
        int totalLength = lineLengths.reduce(new Sum());
        List<Integer> collect = lineLengths.collect();
        System.out.println(collect.toString());
        System.out.println(totalLength);
    }
}
