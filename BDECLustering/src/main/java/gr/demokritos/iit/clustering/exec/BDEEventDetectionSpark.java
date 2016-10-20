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

import gr.demokritos.iit.clustering.clustering.BaseSparkClusterer;
import gr.demokritos.iit.clustering.config.BDESparkContextContainer;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingGraphPairsFunc;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingPairsFunc;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;

import java.util.*;

import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;
import gr.demokritos.iit.clustering.util.ArticleGraphCalculator;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.GraphPairGenerationFilterFunction;
import org.apache.spark.api.java.JavaNewHadoopRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.python.PairwiseRDD;
import org.apache.spark.graphx.EdgeRDD;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.VertexRDD;
import org.apache.spark.rdd.RDD;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Sentence;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;
import scala.Tuple2;
import scala.Tuple4;
import scala.reflect.ClassTag;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */


public class BDEEventDetectionSpark {

    //    private final SparkContext sc;
    private final BDESparkContextContainer sp;

    public BDEEventDetectionSpark(BDESparkContextContainer bdes) {
        this.sp = bdes;
    }

    public JavaSparkContext getContext() {
        return sp.getContext();
    }

    public static void main(String[] args) {

        // we require one argument, the config file
        if (args.length < 1 || args.length > 1) {
            throw new IllegalArgumentException(String.format("USAGE: %s <PATH_TO_CONFIGURATION_FILE>" +
                    "\n\te.g. %s ./res/clustering.properties", BDEEventDetection.class.getName(), BDEEventDetection.class.getName()));
        }

        // call a non-spark event detection method for testing.
        // copied from demo event detection

        String properties = "./res/clustering.properties";
        if (args.length == 1) {
            properties = args[0];
        }

        // init configuration
        ISparkConf conf = new BDESparkConf(args[0]);
        // init sparkConf (holds the spark context object)
        BDESparkContextContainer bdes = new BDESparkContextContainer(conf);
        // instantiate us
        BDEEventDetectionSpark bdedet = new BDEEventDetectionSpark(bdes);
        bdedet.getContext().setLogLevel("ERROR");
                // keep context to pass around
        JavaSparkContext sc = bdedet.getContext();
        // get the spark repository class
        CassandraSparkRepository repo = new CassandraSparkRepository(JavaSparkContext.toSparkContext(sc), conf);

        // get a timestamp : TODO: FIXME
        long timestamp = repo.getLatestTimestamp("event_detection_log");
        System.out.println("timestamp: " + new Date(timestamp).toString());
        System.out.println("LOADING ARTICLES");
        // load batch. The quadruple represents <entry_url, title, clean_text, timestamp>
        // entry URL is supposed to be the unique identifier of an article
        // (though for reuters many articles with same body
        // are republished under different URLs)

        // load articles as RDD 4-tuples
        JavaRDD<Tuple4<String, String, String, Long>> articles = repo.loadArticlesPublishedLaterThan(timestamp);
        BaseSparkClusterer clusterer = new BaseSparkClusterer(JavaSparkContext.toSparkContext(sc),conf.getSimilarityMode(),conf.getCutOffThreshold(), conf.getNumPartitions());

        DemoClusteringFactory fact = new DemoClusteringFactory(conf);
        DemoCassandraRepository repoL = fact.createDemoCassandraRepository();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);
        long ts = now.getTimeInMillis();
        List<BDEArticle> articlesLocally = repoL.loadArticlesAsDemo_crawledInfo(ts,5);


        // ---------------------------------
        // precompute graphs, "on the driver"
        // ---------------------------------
//        List<Graph<String,Object>> graphsList = new ArrayList<>();
//        RDD<Tuple2<RDD,RDD>> verticesEdges = null;
//
//        for (BDEArticle article : articlesLocally)
//        {
//            StringEntity ent1 = new StringEntity();
//            String title = articlesLocally.get(0).getTitle();
//            String text = articlesLocally.get(0).getDescription();
//            ent1.setString(JavaSparkContext.toSparkContext(sc), new StringBuilder().append(title).append(" ").append(text).toString());
//            NGramGraphCreator ngc = new NGramGraphCreator(JavaSparkContext.toSparkContext(sc), conf.getNumPartitions(), 3, 3);
//            graphsList.add(ngc.getGraph(ent1));
//
//
////            Graph<String,Object> mygraph = graphsList.get(0);
////            RDD temp = mygraph.vertices();
////            ClassTag<RDD<Tuple2<String,String>>> tag = scala.reflect.ClassTag$.MODULE$.apply(RDD.class);
////            RDD<Tuple2<String,String>> pair =  temp.cartesian(temp,tag);
////            System.out.println("Vertex count : " + mygraph.vertices().count());
////            System.out.println("NUm in count : " + pair.count());
//        }
//
//        // generate the pairs
//        ArrayList<Tuple2<Graph<String,Object>,Graph<String,Object>>> graphsTuples = new ArrayList<>();
//        ArrayList<Tuple2<BDEArticle,BDEArticle>> articleTuples = new ArrayList<>();
//        for(int i=0;i<graphsList.size();++i)
//        {
//            for(int j=i+1;j<graphsList.size();++j) {
//                graphsTuples.add(new Tuple2(graphsList.get(i), graphsList.get(j)));
//                articleTuples.add(new Tuple2(articlesLocally.get(i),articlesLocally.get(j)));
//            }
//        }
//        //JavaRDD<Tuple2<Graph<String, Object>,Graph<String, Object>>>  graphsTuplesRDD =  sc.parallelize(graphsTuples);
//        //JavaRDD<Tuple2<BDEArticle,BDEArticle>> articleTuplesRDD = sc.parallelize(articleTuples);
//        clusterer.calculateClusters_graphs(articleTuples,graphsTuples);
//
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

        // ---------------------------------
        // end of precompute graphs, "on the driver"
        // ---------------------------------

        // ----------------------------
        // // precompute graphs approach : compute the graph for each article, THEN upload to spark
        // workers so they don't compute the article's graph for each combo
        // ----------------------------
        // this below seems to produce null graph objects (its fields are null)
        // however in the converter class they are fine
        // perhaps the Function interface cannot return non-primitive objects or needs a specific setting
        JavaRDD<Graph<String, Object>> graphs = articles.map(new ArticleGraphCalculator(JavaSparkContext.toSparkContext(sc),conf.getNumPartitions()));
        // force computation
        System.out.println("Graphs are : " + graphs.count());


        // the difficulty is handled badly here: equality checks just # of verticles and edges.
        long startTime = System.currentTimeMillis();
        JavaPairRDD<Graph<String,Object>,Graph<String,Object>> graphPairs = graphs.cartesian(graphs).filter(new GraphPairGenerationFilterFunction());
        long endTime = System.currentTimeMillis();

        System.out.println("Graph pair filtering took " + Long.toString((endTime - startTime)/1000l) + " sec");

        System.out.println("Graphpairs count : " + graphPairs.count());
        JavaRDD<Boolean> matchesrdd = graphPairs.map(
                new ExtractMatchingGraphPairsFunc(conf.getSimilarityMode(),conf.getCutOffThreshold(),conf.getNumPartitions()));
        List<Boolean> matches = matchesrdd.collect();
        int cccc=0;
        for(Boolean b : matches)
        {
            System.out.println(cccc++ + " " + b.toString());

        }
        clusterer.calculateClusters_graphs_2(articles,matches);
        // there is a difficulty computing combinations  of graphs l4ike above
        //  we want to do filtering to keep only the combinations from the cartesian product
        // but we cannot use an 'equals' operator on the graph objects
        // doing it manually

        List<Graph<String,Object>> graphsList = graphs.collect();

        ArrayList<Tuple2<Graph<String, Object>, Graph<String, Object>>> graphsTuples = new ArrayList<>();

        for (int i = 0; i < graphsList.size(); ++i) {
            for (int j = i + 1; j < graphsList.size(); ++j) {
                graphsTuples.add(new Tuple2(graphsList.get(i), graphsList.get(j)));

                System.out.println(i + " - " + j);
            }
        }

        // puth the pairs to spark
        JavaRDD<Tuple2<Graph<String, Object>, Graph<String, Object>>> graphsTuplesRDD = sc.parallelize(graphsTuples);
        // cluster
        clusterer.calculateClusters_graphs_(articles, graphsTuplesRDD);
        // get them back
        List<Tuple2<Graph<String, Object>, Graph<String, Object>>> c = graphsTuplesRDD.collect();
        if (c.get(0)._1().edges() == null) System.out.println("it's null");


//
//        // ----------------------------
//        // end of precompute graph approach
//        // ----------------------------


        // ----------------------------
        // default approach:
        // generate article pairs, for each pair compute the graph and compare, on the fly
        // ----------------------------
        System.out.println("DEFAUTL APPROACH");
        startTime = System.currentTimeMillis();

        JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> articlePairs
                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());
        endTime = System.currentTimeMillis();
        System.out.println("Article pair filtering took " + Long.toString((endTime - startTime)/1000l) + " sec");
        // generate clusters

        clusterer.calculateClusters(articles);

        // ----------------------------
        // end of default approach
        // ----------------------------



        Map<String, Topic> res = clusterer.getArticlesPerCluster();
        System.out.println("Printing clustering results.");
        for(String clustid : res.keySet())
        {
            System.out.println("cluster " + clustid);
            for(Article art : res.get(clustid))
            {
                System.out.println("\t art" + art.toString());
            }
        }


        return;

    }

    private static Map<String, Map<String, String>> getPlaceMappings(List<BDEArticle> articles, Map<String, Topic> clusters) {

        Map<String, BDEArticle> mapped_articles = getMappingPerSourceURL(articles);

        Map<String, Map<String, String>> res = new HashMap();

        for (Map.Entry<String, Topic> entry : clusters.entrySet()) {
            String topic_id = entry.getKey();
            Topic topic = entry.getValue();
            Map<String, String> places_polygons = new HashMap();

            for (Article each : topic) {
                BDEArticle tmp = mapped_articles.get(each.getSource());
                if (tmp != null) {
                    places_polygons.putAll(tmp.getPlaces_to_polygons());
                }
            }
            res.put(topic_id, places_polygons);
        }
        return res;
    }
    private static Map<String, BDEArticle> getMappingPerSourceURL(List<BDEArticle> articles) {
        Map<String, BDEArticle> res = new HashMap();
        for (BDEArticle each : articles) {
            res.put(each.getSource(), each);
        }
        return res;
    }

    private static Map<String, String> convertToPlainSummaries(Map<String, Summary> summaries) {
        Map<String, String> res = new HashMap();
        for (Map.Entry<String, Summary> entry : summaries.entrySet()) {
            Summary tmp = entry.getValue();
            res.put(entry.getKey(), toPlainText(tmp));
        }
        return res;
    }

    private static String toPlainText(Summary tmp) {
        StringBuilder sb = new StringBuilder();
        Summary.SummaryData commander_data = tmp.asSummaryData();
        for (Sentence sen : commander_data.getAllSentences()) {
            String sentSnip = sen.getSnippet().trim();
            if (sentSnip.endsWith(".")) {
                sb.append(sentSnip).append("\n");
            } else {
                sb.append(sentSnip).append(".").append("\n");
            }
        }
        return sb.toString();
    }
    private static Map<String, Long> getTweetClustersToIDsMappings(CleanResultCollection<TwitterResult> cleanTweets) {
        Map<String, Long> res = new HashMap();

        for (TwitterResult cleanTweet : cleanTweets) {
            long post_id = cleanTweet.getTweetID();
            String permalink = cleanTweet.getURL();
            res.put(permalink, post_id);
        }
        return res;
    }
    private static Map<String, String> getTweetClustersToUsersMappings(CleanResultCollection<TwitterResult> cleanTweets) {
        Map<String, String> res = new HashMap();

        for (TwitterResult cleanTweet : cleanTweets) {
            String user = cleanTweet.getUser_name();
            String permalink = cleanTweet.getURL();
            res.put(permalink, user);
        }
        return res;
    }



}
