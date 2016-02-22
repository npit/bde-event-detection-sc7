/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.ext.examples;

import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.exec.BDEEventDetection;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.spark.api.java.*;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.sql.DataFrame;
import scala.Tuple3;

public class LDAExample {

    public static void main(String[] args) {

        ISparkConf conf = new BDESparkConf();
        BDESpark bdes = new BDESpark(conf);

        BDEEventDetection bdedet = new BDEEventDetection(bdes);

        SparkContext sc = bdedet.getContext();

        CassandraSparkRepository repo = new CassandraSparkRepository(sc, conf.getCassandraKeyspace());

        long timestamp = repo.getLatestTimestamp("event_detection_log"); // TODO: add table.
        System.out.println(new Date(timestamp).toString());

        JavaRDD<Tuple3<String, String, Long>> data = repo.loadArticlesPublishedLaterThan(timestamp);
        System.out.println("data: " + data.count());
//        SparkConf conf = new SparkConf().setAppName("LDA Example");
//        JavaSparkContext sc = new JavaSparkContext(conf);
        // Load and parse the data
//        String path = "data/mllib/sample_lda_data.txt"; 
        // TODO: we need wordcount vectors that represent our data
//        JavaRDD<String> data = sc.textFile(path);

        JavaRDD<String> flatMap = data.flatMap(new FlatMapFunction<Tuple3<String, String, Long>, String>() {

            @Override
            public Iterable<String> call(Tuple3<String, String, Long> t) throws Exception {
                Tuple3<String, String, Long> copy = t.copy(t._1(), t._2(), t._3());
                String clean = copy._2();
                String[] c = clean.trim().replaceAll("[.,:;!?@'\"<>\\|]", " ").split("\\s+");
                List<String> asList = Arrays.asList(c);
                return new ArrayList(asList);
            }
        });
        flatMap.collect();
        System.out.println("count:" + flatMap.count());
        List<String> take = flatMap.take(20);
        System.out.println(take.toString());

//        DataFrame d = new DataFrame
        
//        HashingTF htf = new HashingTF();
//        JavaRDD<Vector> parsedData = htf.transform(
//        JavaRDD<Vector> parsedData = htf.transform(flatMap);
//        System.out.println(parsedData.take(10).toString());
//        // Index documents with unique IDs
//        JavaPairRDD<Long, Vector> corpus = JavaPairRDD.fromJavaRDD(parsedData.zipWithIndex().map(
//                new Function<Tuple2<Vector, Long>, Tuple2<Long, Vector>>() {
//                    @Override
//                    public Tuple2<Long, Vector> call(Tuple2<Vector, Long> doc_id) {
//                        return doc_id.swap();
//                    }
//                }
//        ));
//        List<Tuple2<Long, Vector>> take1 = corpus.take(10);
//        System.out.println(take1.toString());
//        corpus.cache();
//
//        // Cluster the documents into three topics using LDA
//        DistributedLDAModel ldaModel = (DistributedLDAModel) new LDA().setK(10).run(corpus);
//
//        // Output topics. Each is a distribution over words (matching word count vectors)
//        System.out.println("Learned topics (as distributions over vocab of " + ldaModel.vocabSize()
//                + " words):");
//        Matrix topics = ldaModel.topicsMatrix();
//        for (int topic = 0; topic < 3; topic++) {
//            System.out.print("Topic " + topic + ":");
//            for (int word = 0; word < ldaModel.vocabSize(); word++) {
//                System.out.print(" " + topics.apply(word, topic));
//            }
//            System.out.println();
//        }
//
//        ldaModel.save(sc, "./myLDAModel");
//        DistributedLDAModel sameModel = DistributedLDAModel.load(sc, "myLDAModel");
    }
}
