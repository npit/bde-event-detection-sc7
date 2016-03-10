/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.exec;

import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class TestParallelNGG {

    public static void main(String[] args) {
        //        ISparkConf conf = new BDESparkConf("spark.properties");
        ISparkConf conf = new BDESparkConf();
        BDESpark bdes = new BDESpark(conf);

        BDEEventDetection bdedet = new BDEEventDetection(bdes);

        SparkContext sc = bdedet.getContext();
        RDD<String> s1 = sc.textFile("/home/gkioumis/Programming/scala/ParallelNGGFork/res/test1", 3);
        RDD<String> s2 = sc.textFile("/home/gkioumis/Programming/scala/ParallelNGGFork/res/test2", 3);
        
        
        
                
        
        
        
    }
}
