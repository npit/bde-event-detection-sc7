/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.ext.other;

import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.CassandraRow;
import com.datastax.spark.connector.japi.rdd.CassandraJavaRDD;
import java.util.List;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassyTest {

    public static void main(String[] args) {
        String appName = "cassy";
        String master = "local[2]";
        SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
        JavaSparkContext sc = new JavaSparkContext(conf);

        CassandraJavaRDD<CassandraRow> items
//                = CassandraJavaUtil.javaFunctions(sc).cassandraTable("bde", "news_articles").select("entry_url").where("reversed_host", "com.reuters.feeds").limit(5l);
                = CassandraJavaUtil.javaFunctions(sc).cassandraTable("bde", "news_articles").select("entry_url").limit(5l);

        List<CassandraRow> collect = items.collect();

        for (CassandraRow each : collect) {
            System.out.println(each.getString("entry_url"));
        }
        sc.close();
    }
}
