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

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.IBaseRepository;
import gr.demokritos.iit.clustering.clustering.BaseSparkClusterer;
import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingPairsFunc;
import gr.demokritos.iit.clustering.newsum.IClusterer;
import gr.demokritos.iit.clustering.newsum.NSClusterer;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;

import java.util.*;
import java.util.logging.Level;

import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.asset.social.classification.IClassifier;
import org.scify.asset.social.clustering.SocialMediaClusterer;
import org.scify.asset.social.data.preprocessing.DefaultSocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.ISocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.IStemmer;
import org.scify.asset.social.data.preprocessing.TwitterStemmer;
import org.scify.newsum.server.clustering.ArticleMCLClusterer;
import org.scify.newsum.server.clustering.BaseArticleClusterer;
import org.scify.newsum.server.clustering.IArticleClusterer;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Sentence;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.nlp.sentsplit.DefaultSentenceSplitter;
import org.scify.newsum.server.nlp.sentsplit.ISentenceSplitter;
import org.scify.newsum.server.summarization.ISummarizer;
import org.scify.newsum.server.summarization.Summarizer;
import scala.Tuple2;
import scala.Tuple4;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDEEventDetectionSpark {

    //    private final SparkContext sc;
    private final BDESpark sp;

    public BDEEventDetectionSpark(BDESpark bdes) {
        this.sp = bdes;
    }

    public SparkContext getContext() {
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
        BDESpark bdes = new BDESpark(conf);
        // instantiate us
        BDEEventDetectionSpark bdedet = new BDEEventDetectionSpark(bdes);
        // keep context to pass around
        SparkContext sc = bdedet.getContext();
        // get the spark repository class
        CassandraSparkRepository repo = new CassandraSparkRepository(sc, conf);
        // get a timestamp : TODO: FIXME
        long timestamp = repo.getLatestTimestamp("event_detection_log"); // TODO: add table(?) or use parameter days_back.
        System.out.println("timestamp: " + new Date(timestamp).toString());
        System.out.println("LOADING ARTICLES");
        // load batch. The quadruple represents <entry_url, title, clean_text, timestamp>
        // entry URL is supposed to be the unique identifier of an article (though for reuters many articles with same body
        // are republished under different URLs)

        // load articles as RDD 4-tuples
        JavaRDD<Tuple4<String, String, String, Long>> RDDbatch = repo.loadArticlesPublishedLaterThan(timestamp);
        StructUtils.printArticles(RDDbatch);
        // generate all article pairs
        BaseSparkClusterer clusterer = new BaseSparkClusterer(sc,conf.getSimilarityMode(),conf.getCutOffThreshold(), conf.getNumPartitions());
        clusterer.calculateClusters(RDDbatch);

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

        int a=2;
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
