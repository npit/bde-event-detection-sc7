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
//
//import gr.demokritos.iit.clustering.clustering.BaseSparkClusterer;
//import gr.demokritos.iit.clustering.clustering.BaseSparkClusterer2;
//import gr.demokritos.iit.clustering.config.BDESparkContextContainer;
//import gr.demokritos.iit.clustering.config.BDESparkConf;
//import gr.demokritos.iit.clustering.config.ISparkConf;
//import gr.demokritos.iit.clustering.model.BDEArticle;
//import gr.demokritos.iit.clustering.newsum.ExtractMatchingGraphPairsFunc;
////import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
////import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
//import gr.demokritos.iit.clustering.repository.ClusteringCassandraSparkRepository;
//
//import java.util.*;
//
//import gr.demokritos.iit.clustering.util.ArticleGraphCalculator;
//import gr.demokritos.iit.clustering.util.GraphPairGenerationFilterFunction;
//import org.apache.spark.api.java.JavaPairRDD;
//import org.apache.spark.api.java.JavaRDD;
//import org.apache.spark.api.java.JavaSparkContext;
//import org.apache.spark.graphx.Graph;
//import org.scify.asset.server.model.datacollections.CleanResultCollection;
//import org.scify.asset.server.model.structures.social.TwitterResult;
//import org.scify.newsum.server.model.structures.Article;
//import org.scify.newsum.server.model.structures.Sentence;
//import org.scify.newsum.server.model.structures.Summary;
//import org.scify.newsum.server.model.structures.Topic;
//import scala.Tuple4;
//
///**
// * @author George K. <gkiom@iit.demokritos.gr>
// */
//
//
//public class BDEEventDetectionSpark {
//
//    //    private final SparkContext sc;
//    private final BDESparkContextContainer sp;
//
//    public BDEEventDetectionSpark(BDESparkContextContainer bdes) {
//        this.sp = bdes;
//    }
//
//    public JavaSparkContext getContext() {
//        return sp.getContext();
//    }
//
//    public static void main(String[] args) {
//
//        // we require one argument, the config file
//        if (args.length < 1 || args.length > 1) {
//            throw new IllegalArgumentException(String.format("USAGE: %s <PATH_TO_CONFIGURATION_FILE>" +
//                    "\n\te.g. %s ./res/clustering.properties", BDEEventDetection.class.getName(), BDEEventDetection.class.getName()));
//        }
//
//        // call a non-spark event detection method for testing.
//        // copied from demo event detection
//
//        String properties = "./res/clustering.properties";
//        if (args.length == 1) {
//            properties = args[0];
//        }
//
//        // init configuration
//        ISparkConf conf = new BDESparkConf(args[0]);
//        // init sparkConf (holds the spark context object)
//        BDESparkContextContainer bdes = new BDESparkContextContainer(conf);
//        // instantiate us
//        BDEEventDetectionSpark bdedet = new BDEEventDetectionSpark(bdes);
//        bdedet.getContext().setLogLevel("ERROR");
//                // keep context to pass around
//        JavaSparkContext sc = bdedet.getContext();
//        // get the spark repository class
//        ClusteringCassandraSparkRepository repo = new ClusteringCassandraSparkRepository(JavaSparkContext.toSparkContext(sc), conf);
//        long timestamp = repo.getLatestTimestamp("event_detection_log");
//
//
//
//        long startTotal = System.currentTimeMillis();
//        // Cluster with a predefined method
//        // -----------------------------------
//
//        //sparkClusteringWithPrecomputeGraphs(repo,timestamp,conf,sc);
//        //sparkClusteringDefault(repo,timestamp,conf,sc);
//        sparkClustering_JohnApproach(repo,timestamp,conf,sc);
//        // -----------------------------------
//        long endTotal  = System.currentTimeMillis();
//        System.out.println( "Total clustering time : " + Long.toString((endTotal - startTotal)/1000l) + "sec" );
//
//        return;
//
//    }
//    private static void sparkClustering_JohnApproach(ClusteringCassandraSparkRepository repo, long timestamp, ISparkConf conf, JavaSparkContext sc)
//    {
//        System.out.println("************ Serial graph creation approach");
//
//        JavaRDD<Tuple4<String, String, String, Long>> articles = repo.loadArticlesPublishedLaterThan(timestamp);
//        BaseSparkClusterer2 clust = new BaseSparkClusterer2(JavaSparkContext.toSparkContext(sc),conf.getSimilarityMode(),conf.getCutOffThreshold(),conf.getNumPartitions());
//        clust.calculateClusters(articles);
//
//
//        Map<String, Topic> res = clust.getArticlesPerCluster();
//        System.out.println("Printing clustering results.");
//        for(String clustid : res.keySet())
//        {
//            System.out.println("cluster " + clustid);
//            for(Article art : res.get(clustid))
//            {
//                System.out.println("\t art" + art.toString());
//            }
//        }
//    }
//    private static void sparkClusteringDefault(ClusteringCassandraSparkRepository repo, long timestamp, ISparkConf conf, JavaSparkContext sc)
//    {
//        System.out.println("*********  Default approach");
//        JavaRDD<Tuple4<String, String, String, Long>> articles = repo.loadArticlesPublishedLaterThan(timestamp);
//        BaseSparkClusterer clusterer = new BaseSparkClusterer(JavaSparkContext.toSparkContext(sc),conf.getSimilarityMode(),conf.getCutOffThreshold(), conf.getNumPartitions());
//
//        System.out.println("DEFAUTL APPROACH");
//        // generate clusters
//        clusterer.calculateClusters(articles);
//
//        Map<String, Topic> res = clusterer.getArticlesPerCluster();
//        System.out.println("Printing clustering results.");
//        for(String clustid : res.keySet())
//        {
//            System.out.println("cluster " + clustid);
//            for(Article art : res.get(clustid))
//            {
//                System.out.println("\t art" + art.toString());
//            }
//        }
//    }
//    private static void sparkClusteringWithPrecomputeGraphs(ClusteringCassandraSparkRepository repo, long timestamp, ISparkConf conf, JavaSparkContext sc)
//    {
//        System.out.println("************ Precomputed graphs approach");
//        JavaRDD<Tuple4<String, String, String, Long>> articles = repo.loadArticlesPublishedLaterThan(timestamp);
//        BaseSparkClusterer clusterer = new BaseSparkClusterer(JavaSparkContext.toSparkContext(sc),conf.getSimilarityMode(),conf.getCutOffThreshold(), conf.getNumPartitions());
//
//        Calendar now = Calendar.getInstance();
//        now.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);
//
//        System.out.println("Calculating graphs.");
//        long startTime = System.currentTimeMillis();
//        JavaRDD<Graph<String, Object>> graphs = articles.map(new ArticleGraphCalculator(JavaSparkContext.toSparkContext(sc),conf.getNumPartitions()));
//
//        System.out.print(graphs.count() + " Graphs calculated");
//        long endTime = System.currentTimeMillis();
//        System.out.println(" in : " + Long.toString((endTime - startTime)/1000l) + " sec");
//        System.out.println( " Mapping to boolean similarity.");
//
//        // the difficulty is handled badly here: equality checks just # of verticles and edges.
//        startTime = System.currentTimeMillis();
//        JavaPairRDD<Graph<String,Object>,Graph<String,Object>> graphPairs = graphs.cartesian(graphs).filter(new GraphPairGenerationFilterFunction());
//        System.out.print( graphPairs.count() + " Graph-pairs filtered.");
//        endTime = System.currentTimeMillis();
//
//        System.out.println("Graph pair filtering took " + Long.toString((endTime - startTime)/1000l) + " sec");
//
//        startTime = System.currentTimeMillis();
//        JavaRDD<Boolean> matchesrdd = graphPairs.map(
//                new ExtractMatchingGraphPairsFunc(conf.getSimilarityMode(),conf.getCutOffThreshold(),conf.getNumPartitions()));
//        List<Boolean> matches = matchesrdd.collect();
//        endTime = System.currentTimeMillis();
//        System.out.println("Mapping to boolean, took " + Long.toString((endTime - startTime)/1000l) + " sec");
//
//        int cccc=0;
//        for(Boolean b : matches)
//        {
//            System.out.println(cccc++ + " " + b.toString());
//
//        }
//        clusterer.calculateClusters_graphs_2(articles,matches);
//
//
//        Map<String,Topic> articlesPerCluster = clusterer.getArticlesPerCluster();
//        System.out.println("Printing clustering results.");
//        for(String clustid : articlesPerCluster.keySet())
//        {
//            System.out.println("cluster " + clustid);
//            for(Article art : articlesPerCluster.get(clustid))
//            {
//                System.out.println("\t art" + art.toString());
//            }
//        }
////        if(fact != null)
////            fact.releaseResources();
//
//    }
//    private static Map<String, Map<String, String>> getPlaceMappings(List<BDEArticle> articles, Map<String, Topic> clusters) {
//
//        Map<String, BDEArticle> mapped_articles = getMappingPerSourceURL(articles);
//
//        Map<String, Map<String, String>> res = new HashMap();
//
//        for (Map.Entry<String, Topic> entry : clusters.entrySet()) {
//            String topic_id = entry.getKey();
//            Topic topic = entry.getValue();
//            Map<String, String> places_polygons = new HashMap();
//
//            for (Article each : topic) {
//                BDEArticle tmp = mapped_articles.get(each.getSource());
//                if (tmp != null) {
//                    places_polygons.putAll(tmp.getPlaces_to_polygons());
//                }
//            }
//            res.put(topic_id, places_polygons);
//        }
//        return res;
//    }
//    private static Map<String, BDEArticle> getMappingPerSourceURL(List<BDEArticle> articles) {
//        Map<String, BDEArticle> res = new HashMap();
//        for (BDEArticle each : articles) {
//            res.put(each.getSource(), each);
//        }
//        return res;
//    }
//
//    private static Map<String, String> convertToPlainSummaries(Map<String, Summary> summaries) {
//        Map<String, String> res = new HashMap();
//        for (Map.Entry<String, Summary> entry : summaries.entrySet()) {
//            Summary tmp = entry.getValue();
//            res.put(entry.getKey(), toPlainText(tmp));
//        }
//        return res;
//    }
//
//    private static String toPlainText(Summary tmp) {
//        StringBuilder sb = new StringBuilder();
//        Summary.SummaryData commander_data = tmp.asSummaryData();
//        for (Sentence sen : commander_data.getAllSentences()) {
//            String sentSnip = sen.getSnippet().trim();
//            if (sentSnip.endsWith(".")) {
//                sb.append(sentSnip).append("\n");
//            } else {
//                sb.append(sentSnip).append(".").append("\n");
//            }
//        }
//        return sb.toString();
//    }
//    private static Map<String, Long> getTweetClustersToIDsMappings(CleanResultCollection<TwitterResult> cleanTweets) {
//        Map<String, Long> res = new HashMap();
//
//        for (TwitterResult cleanTweet : cleanTweets) {
//            long post_id = cleanTweet.getTweetID();
//            String permalink = cleanTweet.getURL();
//            res.put(permalink, post_id);
//        }
//        return res;
//    }
//    private static Map<String, String> getTweetClustersToUsersMappings(CleanResultCollection<TwitterResult> cleanTweets) {
//        Map<String, String> res = new HashMap();
//
//        for (TwitterResult cleanTweet : cleanTweets) {
//            String user = cleanTweet.getUser_name();
//            String permalink = cleanTweet.getURL();
//            res.put(permalink, user);
//        }
//        return res;
//    }
//
//
//
//}
