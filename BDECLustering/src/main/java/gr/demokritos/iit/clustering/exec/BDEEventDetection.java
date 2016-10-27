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

import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.clustering.config.*;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;

import java.util.*;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.asset.social.classification.IClassifier;
import org.scify.asset.social.clustering.SocialMediaClusterer;
import org.scify.asset.social.data.preprocessing.DefaultSocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.ISocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.IStemmer;
import org.scify.asset.social.data.preprocessing.TwitterStemmer;
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

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDEEventDetection {

    //    private final SparkContext sc;
    private final BDESparkContextContainer sp;

    public BDEEventDetection(BDESparkContextContainer bdes) {
        this.sp = bdes;
    }

    public JavaSparkContext getContext() {
        return sp.getContext();
    }

    public static void main(String[] args) {


        // we require one argument, the config file
        if (args.length < 1 ) {
            throw new IllegalArgumentException(String.format("USAGE: %s <PATH_TO_CONFIGURATION_FILE> " +
                    "\n\te.g. %s ./res/clustering.properties", BDEEventDetection.class.getName(), BDEEventDetection.class.getName()));
        }

        // call a non-spark event detection method for testing.
        // copied from demo event detection

        String properties = "res/clustering.properties";
        if (args.length >= 1) {
            properties = args[0];
        }

        // load base configuration, initialize repository
        IClusteringConf configuration = new clusteringConf(properties);
        boolean SendToStrabon = configuration.sendToStrabon();
        boolean onlySendToStrabon = configuration.justSendToStrabon();


        DemoCassandraRepository repository;
        DemoClusteringFactory factory = new DemoClusteringFactory(configuration);
        repository = factory.createDemoCassandraRepository();

        // just send to strabon, if that mode is specified
        if(onlySendToStrabon)
        {
	    String strabonURL=configuration.getStrabonURL();
            System.out.print("Note: No clustering: will only send events to strabon, to url:["+strabonURL+"].");
            repository.storeAndChangeDetectionEvents(strabonURL);
            if(factory != null)
            {
                System.out.println("Releasing resources.");
                factory.releaseResources();
            }
            return;
        }
        // specify the range of news articles to extract from, for clustering
        Calendar cal = Utils.getCalendarFromStringTimeWindow(configuration.getDocumentRetrievalTimeWindow());
//        Calendar now = Calendar.getInstance();
//        now.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);
        System.out.println("calendar retrieval setting: " + cal.getTime());


        String clusteringMode = configuration.getClusteringMode();
        long tstamp = cal.getTimeInMillis();
        System.out.println("loading articles");
        long startTime = System.currentTimeMillis();
        List<BDEArticle> articles = repository.loadArticlesAsDemo_crawledInfo(tstamp, configuration.getMaxNumberOfArticles());
        long endTime = System.currentTimeMillis();
        System.out.println("Took " + Long.toString((endTime - startTime)/1000l) + " sec");

        // clusterer
        IArticleClusterer cl = new BaseArticleClusterer(articles);
        System.out.println("clustering articles...");
        startTime = System.currentTimeMillis();
        cl.calculateClusters();
        endTime = System.currentTimeMillis();
        System.out.println("Took " + Long.toString((endTime - startTime)/1000l) + " sec");

        Map<String,Topic> articlesPerCluster = cl.getArticlesPerCluster();

        // the below should be already populated after news crawls
        Map<String, Map<String, String>> place_mappings = getPlaceMappings(articles, articlesPerCluster);



        ISentenceSplitter splitter = new DefaultSentenceSplitter(configuration.getSentenceSplitterModelPath());
        System.out.println("getting summaries");
        ISummarizer sum = new Summarizer(splitter);
        // get summaries
        Map<String, Summary> summaries = sum.getSummaries(new HashSet(articlesPerCluster.values()));
        System.out.println("loading tweets");
        // get token dictionary from topics

        // process tweets
        Collection<TwitterResult> tweets = repository.loadTweetsAsDemo(tstamp);
        // clean tweets (stem)
        IStemmer tsStemmer = new TwitterStemmer(1, configuration.getStopwordsFilePath());
        System.out.println("Creating tweets dictionary");
        ISocialMediaCleaner social_media_cleaner = new DefaultSocialMediaCleaner(tsStemmer);
        Map<String, String> plainTextSummaries = convertToPlainSummaries(summaries);
        Set<String> summaries_dict_words = social_media_cleaner.createDictionaryFromSummaries(plainTextSummaries);
        System.out.println("Cleaning tweets");
        CleanResultCollection<TwitterResult> cleanTweets
                = social_media_cleaner.cleanTweets((List<TwitterResult>) tweets, summaries_dict_words);
        System.out.println(cleanTweets.size() + " tweets left after cleaning" );
        System.out.println("Clustering tweets...");
        // get social media clusters
        IArticleClusterer smClusterer = factory.getSocialMediaClustererForTwitter(SocialMediaClusterer.Mode.NVS, cleanTweets);
        smClusterer.calculateClusters();
        Collection<Topic> tweetClusters = smClusterer.getArticlesPerCluster().values();
        System.out.println("Classifying tweets...");
        //IClassifier smClassifier = factory.getSocialMediaClassifierForTwitter(plainTextSummaries, tweetClusters, tsStemmer);
        // default thresholds are

        double min_assign_sim_threshold = configuration.getTwitterMappingSimilarityThreshold();
        double min_assign_titlesim_threshold = configuration.getTwitterMappingTitleSimilarityThreshold();
        System.out.println("Text similarity thesh:" + min_assign_sim_threshold );
        System.out.println("Text title similarity thesh:" + min_assign_titlesim_threshold );
        IClassifier smClassifier = factory.getSocialMediaClassifierForTwitter(min_assign_sim_threshold, min_assign_titlesim_threshold,plainTextSummaries, tweetClusters, tsStemmer);
        Map<Topic, List<String>> related = smClassifier.getRelated();

        Map<String, Long> tweetURLtoPostIDMapping = getTweetClustersToIDsMappings(cleanTweets);
        Map<String, String> tweetURLtoUserMapping = getTweetClustersToUsersMappings(cleanTweets);
        System.out.println("saving events...");

        ArrayList<ArrayList<Object>> storedEvents = repository.saveEvents(articlesPerCluster, summaries, related, place_mappings, tweetURLtoPostIDMapping, tweetURLtoUserMapping, 2);

        if (SendToStrabon) {
	    String strabonURL=configuration.getStrabonURL();
            System.out.print("Finally,sending events to strabon to url ["+strabonURL+"].");
            repository.storeAndChangeDetectionEvents(strabonURL);
        }
        else
        {
            System.out.println("Sending events to Strabon is disabled.");
        }

        if(configuration.getTriggerChangeDetection())
        {
            System.out.println("Checking if change detection should be triggered for the newly generated events.");
            int threshold = configuration.getChangeDetectionThreshold();
            for(int ev = 0; ev < storedEvents.size(); ++ ev)
            {
                // event container , to return event for change detection
                // title, descr, date, tweets, sources, id, placemappings
                ArrayList<Object> currEvent = storedEvents.get(ev);
                if( ((Map<String,String>)currEvent.get(4)).size() > threshold)
                {
                    triggerChangeDetection(currEvent);
                }
            }
        }
        else
        {
            System.out.println("Change detection is disabled.");
        }

        // clean up

        if(factory != null)
        {
            System.out.println("Releasing resources.");
            factory.releaseResources();
        }

        System.out.println("Done");
        return;

    }

    private static void triggerChangeDetection(ArrayList<Object> event)
    {
        // event container , to return event for change detection
        // title, descr, date, tweets, sources, id, placemappings

        // maybe we dont need the event argument ( or event returning the whole events from the saveEvents
        // can be just boolean, if we need to only trigger the remote with no arguments

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
