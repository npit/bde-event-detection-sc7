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
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;

import java.util.*;

import gr.demokritos.iit.clustering.util.ArticleGraphCalculator;
import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.Graph;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Sentence;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;
import scala.Serializable;
import scala.Tuple2;
import scala.Tuple4;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */

class test implements Serializable
{
    public int a;
    public int b;
    public test(int aa,int bb){ a=aa;b=bb;}
}
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

        // // precompute graphs approach
        JavaRDD<Graph<String, Object>> graphs = articles.map(new ArticleGraphCalculator(JavaSparkContext.toSparkContext(sc),conf.getNumPartitions()));
        // force computation
        System.out.println("Graphs are : " + graphs.count());

        // there is a difficulty computing combinations (filter the cartesian) of graphs like above
        // doing it manually
        List<Graph<String,Object>> graphsList = graphs.collect();
        ArrayList<Tuple2<Graph<String,Object>,Graph<String,Object>>> graphsTuples = new ArrayList<>();

        for(int i=0;i<graphsList.size();++i)
        {
            for(int j=i+1;j<graphsList.size();++j) {
                graphsTuples.add(new Tuple2(graphsList.get(i), graphsList.get(j)));

                System.out.println(i + " - " + j);
            }
        }
        // put the graphs to the spark workers

        StructUtils.printArticles(articles);
        // generate all article pairs

        JavaRDD<Tuple2<Graph<String, Object>,Graph<String, Object>>>  graphsTuplesRDD =  sc.parallelize(graphsTuples);

        List<Tuple2<Graph<String, Object>,Graph<String, Object>>> c = graphsTuplesRDD.collect();
        if(c.get(0)._1().edges() == null) System.out.println("it's null");
       // clusterer.calculateClusters_graphs(articles,graphsTuplesRDD);

     // the below works, though.
        ArrayList<test> testlist = new ArrayList<>();
        testlist.add(new test(1,2));
        testlist.add(new test(3,4));

        ArrayList<Tuple2<test,test>> alist = new ArrayList<>();
        alist.add(new Tuple2<test,test>(testlist.get(0),testlist.get(1)));


        JavaRDD<Tuple2<test,test>> parall = sc.parallelize(alist);
        List<Tuple2<test,test>> cc = parall.collect();
        if(cc.get(0)._1() == null) System.out.println("it's null!!");
        if(cc.get(0)._1()== null) System.out.println("it's null!!");
        if(cc.get(0)._1() == null) System.out.println("it's null!!");
        clusterer.calculateClusters_graphs(articles,graphsTuplesRDD);

        clusterer.calculateClusters(articles);

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
