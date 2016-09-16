package gr.demokritos.iit.clustering.exec;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;
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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class DemoBDEEventDetection {

    private static final Logger LOGGER = Logger.getLogger(DemoBDEEventDetection.class.getName());

    /**
     * @param args provide the .properties file (with cassandra credentials, etc).
     */
    public static void main(String[] args) {

        String properties = "./res/clustering.properties";
        if (args.length == 1) {
            properties = args[0];
        }
        IBaseConf conf = new BaseConfiguration(properties);
        DemoClusteringFactory factory = null;
        DemoCassandraRepository repository;
        try {
            factory = new DemoClusteringFactory(conf);
            repository = factory.createDemoCassandraRepository();


            Calendar now = Calendar.getInstance();
            now.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);

            long timestamp = now.getTimeInMillis();
            LOGGER.info("loading articles");
            List<BDEArticle> articles = repository.loadArticlesAsDemo(timestamp);

            IArticleClusterer cl = new BaseArticleClusterer(articles);
            LOGGER.info("clustering articles...");
            cl.calculateClusters();

            // get events
            Map<String, Topic> clusters = cl.getArticlesPerCluster();
            // get location / boundary boxes mappings per topic ID

            Map<String, Map<String, String>> place_mappings = getPlaceMappings(articles, clusters);

            ISentenceSplitter splitter = new DefaultSentenceSplitter(conf.getSentenceSplitterModelPath());
            LOGGER.info("getting summaries");
            ISummarizer sum = new Summarizer(splitter);
            // get summaries
            Map<String, Summary> summaries = sum.getSummaries(new HashSet(clusters.values()));
            LOGGER.info("loading tweets");
            // get token dictionary from topics
            Collection<TwitterResult> tweets = repository.loadTweetsAsDemo(timestamp);
            // clean tweets (stem)
            IStemmer tsStemmer = new TwitterStemmer(1, conf.getStopwordsFilePath());
            LOGGER.info("Creating tweets dictionary");
            ISocialMediaCleaner social_media_cleaner = new DefaultSocialMediaCleaner(tsStemmer);
            Map<String, String> plainTextSummaries = convertToPlainSummaries(summaries);
            Set<String> summaries_dict_words = social_media_cleaner.createDictionaryFromSummaries(plainTextSummaries);
            LOGGER.info("Cleaning tweets");
            CleanResultCollection<TwitterResult> cleanTweets
                    = social_media_cleaner.cleanTweets((List<TwitterResult>) tweets, summaries_dict_words);
            LOGGER.log(Level.INFO, "{0} tweets left after cleaning", cleanTweets.size());
            LOGGER.info("Clustering tweets...");
            // get social media clusters
            IArticleClusterer smClusterer = factory.getSocialMediaClustererForTwitter(SocialMediaClusterer.Mode.NVS, cleanTweets);
            smClusterer.calculateClusters();
            Collection<Topic> tweetClusters = smClusterer.getArticlesPerCluster().values();
            LOGGER.info("Classifying tweets...");
            IClassifier smClassifier = factory.getSocialMediaClassifierForTwitter(plainTextSummaries, tweetClusters, tsStemmer);

            Map<Topic, List<String>> related = smClassifier.getRelated();

            Map<String, Long> tweetURLtoPostIDMapping = getTweetClustersToIDsMappings(cleanTweets);
            Map<String, String> tweetURLtoUserMapping = getTweetClustersToUsersMappings(cleanTweets);

            LOGGER.info("saving events...");

            repository.saveEvents(clusters, summaries, related, place_mappings, tweetURLtoPostIDMapping,tweetURLtoUserMapping, 2);

            LOGGER.info("Sending events to popeye.di.uoa...");
            repository.storeAndChangeDetectionEvents("");
            LOGGER.info("Done");
        } finally {
            if (factory != null) {
                System.out.println("Releasing resources.");
                factory.releaseResources();
                System.out.println("Done.");
            }
        }
    }

    /**
     * @param cleanTweets
     * @return a mapping between the tweet URL and the tweet ID, because this information is lost from the underlying transformations
     */
    private static Map<String, Long> getTweetClustersToIDsMappings(CleanResultCollection<TwitterResult> cleanTweets) {
        Map<String, Long> res = new HashMap();

        for (TwitterResult cleanTweet : cleanTweets) {
            long post_id = cleanTweet.getTweetID();
            String permalink = cleanTweet.getURL();
            res.put(permalink, post_id);
        }
        return res;
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
