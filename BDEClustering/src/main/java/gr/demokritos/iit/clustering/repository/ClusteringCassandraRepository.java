package gr.demokritos.iit.clustering.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.vividsolutions.jts.io.ParseException;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.clustering.clustering.MCLClusterer;
import gr.demokritos.iit.clustering.clustering.ParameterizedBaseArticleClusterer;
import gr.demokritos.iit.clustering.config.IClusteringConf;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.newsum.IClusterer;
import gr.demokritos.iit.location.repository.LocationCassandraRepository;
import gr.demokritos.iit.location.util.GeometryFormatTransformer;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.asset.social.classification.IClassifier;
import org.scify.asset.social.classification.SocialMediaClassifier;
import org.scify.asset.social.clustering.SocialMediaClusterer;
import org.scify.asset.social.data.preprocessing.DefaultSocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.ISocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.IStemmer;
import org.scify.asset.social.data.preprocessing.TwitterStemmer;
import org.scify.newsum.server.clustering.IArticleClusterer;
import org.scify.newsum.server.model.datacollections.Articles;
import org.scify.newsum.server.model.datacollections.Summaries;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static gr.demokritos.iit.base.conf.IBaseConf.DATE_FORMAT_ISO_8601;
import static gr.demokritos.iit.base.conf.IBaseConf.TIMEZONE_ID_UTC;
import static gr.demokritos.iit.base.util.Utils.toc;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class ClusteringCassandraRepository extends LocationCassandraRepository implements IClusteringRepository{

    protected IClusteringConf configuration;
    private boolean status;
    protected List<BDEArticle> articles;


    protected HashMap<String, Topic> ArticlesPerCluster;
    Collection<TwitterResult> tweets;
    Map<String, Summary> summaries;
    ArrayList<ArrayList<Object>> SavedEvents;

    Map<Topic, List<String>> RelatedTweets;
    Map<String, Map<String, String>> place_mappings;
    Map<String, Long> tweetURLtoPostIDMapping;
    Map<String, String> tweetURLtoUserIDMapping;

    public ClusteringCassandraRepository(Session session, IClusteringConf configuration)
    {
        super(session);
        this.configuration = configuration;
    }

    // use this class to save topics for DEMO
    // event_size_cuttof: do not save topics with size less than this value
    @Override
    public void localStoreEvents() {

        place_mappings = getPlaceMappings(articles, ArticlesPerCluster);
        int event_size_cuttof = configuration.getEventSizeCutoffThreshold();
        System.out.println("saving events...");

        SavedEvents = new ArrayList<>();

        for (Map.Entry<String, Topic> entry  : ArticlesPerCluster.entrySet()) {
            String id = entry.getKey();
            Topic t = entry.getValue();
            if (t.size() >= event_size_cuttof) {
                ArrayList<Object> ev = saveEvent(id, t, summaries.get(id), RelatedTweets, place_mappings, tweetURLtoPostIDMapping,tweetURLtoUserIDMapping);
                if(! ev.isEmpty())
                    SavedEvents.add(ev);
            }
        }

    }

    public ArrayList<Object> saveEvent(String topicID,
                          Topic t,
                          Summary s,
                          Map<Topic, List<String>> relatedTweets,
                          Map<String, Map<String, String>> places_polygons_per_id,
                          Map<String, Long> tweetURLtoPostIDMapping,
                          Map<String, String> tweetURLtoUserIDMapping

                          ) {


        // event container , to return event for change detection
        // title, descr, date, tweets, sources, id, placemappings
        ArrayList<Object> event = new ArrayList<>();



        String title = t.getTitle();
        System.out.println(String.format("saving event id: %s with title: %s", topicID, title));
        // set top sentence as description
        List<Sentence> sentences = s.asSummaryData().getAllSentences();
        if(sentences.isEmpty())
        {
            System.out.println("Sentences list for event is empty. Skipping.");
            return event;
        }
        String description = sentences.get(0).getSnippet();
        Calendar cDate = t.getDate();
        // get ISO 8601 date format
        String sUTCEventDate = Utils.toTimezoneFormattedStr(cDate, TIMEZONE_ID_UTC, DATE_FORMAT_ISO_8601);
        // get place mappings of the underlying articles
        Map<String, String> place_mappings = places_polygons_per_id.get(topicID);
        // get tweet IDs related to the event
        Map<String, Set<Long>> tweetIDsPerTopicID = extractRelatedTweetIDs(relatedTweets, tweetURLtoPostIDMapping);
        // get all tweet IDs for the topic related
        //Set<Long> tweetIDs = tweetIDsPerTopicID.get(topicID);
        Map<Long,String> tweetIDsUsers = extractRelatedTweetIDsTitlesPerTopicID(topicID,relatedTweets,
                tweetURLtoPostIDMapping,tweetURLtoUserIDMapping);
        if(tweetIDsUsers == null) System.out.println("> The event has no assigned tweets.");
        // news : updated to extract URL + title pairs
        //Set<String> topicSourceURLs = extractSourceURLs(t);
        Map<String,String> topicSourceURL_Titles = extractSourceURLTitlePairs(t);
        // update events
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                .with(set(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName(), title))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_DESCRIPTION.getColumnName(), description))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName(), sUTCEventDate))
                //.and(set(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), place_mappings)) //TODO: insert placemapp iteratively! (below for, most probably)
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(),
                 tweetIDsUsers == null ? Collections.EMPTY_SET : tweetIDsUsers))
               // .and(set(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), topicSourceURLs))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), topicSourceURL_Titles))
                .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), topicID));
        //System.out.println(upsert.toString());
        session.execute(upsert);


        event.add(title);
        event.add(description);
        event.add(sUTCEventDate);
        event.add(tweetIDsUsers);
        event.add(topicSourceURL_Titles);
        event.add(topicID);
        event.add(place_mappings);


        for (Map.Entry<String, String> entry : place_mappings.entrySet()) {
            String place_literal = entry.getKey();
            String polygon = entry.getValue();
            // update events_per_place
            upsert = QueryBuilder
                    .update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS_PER_PLACE.getTableName())
                    .with(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_TITLE.getColumnName(), title))
                    .and(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_DESCRIPTION.getColumnName(), description))
                    .and(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_DATE_LITERAL.getColumnName(), sUTCEventDate))
                    .and(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_PLACE_POLYGON.getColumnName(), polygon))
                    .and(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_TWEET_IDS.getColumnName(),
                            tweetIDsUsers == null ? Collections.EMPTY_SET : tweetIDsUsers))
                    //.and(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_EVENT_SOURCE_URLS.getColumnName(), topicSourceURLs))
                    .and(set(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_EVENT_SOURCE_URLS.getColumnName(), topicSourceURL_Titles))
                    .where(eq(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), place_literal))
                    .and(eq(Cassandra.Event.TBL_EVENTS_PER_PLACE.FLD_EVENT_ID.getColumnName(), topicID));
            //System.out.println(upsert.toString());
            session.execute(upsert);

            // also update regular events, 1 geometry at a time
            upsert = QueryBuilder
                    .update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                    .with(set(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), place_mappings)) //TODO: insert placemapp iteratively! (below for, most probably)
                    .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), topicID));
            //System.out.println(upsert.toString());
            session.execute(upsert);

        }

        return event;
    }

    private Map<Long,String> extractRelatedTweetIDsTitlesPerTopicID(
            String tid,
            Map<Topic, List<String>> relatedTweets,
            Map<String, Long> tweetURLtoPostIDMapping,
            Map<String, String> tweetURLtoUserIDMapping
    ) {
        Map<String, Map<Long,String>> res = new HashMap();

        for (Map.Entry<Topic, List<String>> entry : relatedTweets.entrySet()) {
            Topic tweetsCluster = entry.getKey();
            List<String> lClusterIDs = entry.getValue();

            for (String topicID : lClusterIDs) {
                if (!res.containsKey(topicID)) {
                    Map<Long,String> tweetData = new HashMap();
                    //Set<Long> tweetIDs = new HashSet();

                    // it is a tweet transformed to an article
                    for (Article article : tweetsCluster) {
                        Long id = (tweetURLtoPostIDMapping.get(article.getSource()));
                        String title = (tweetURLtoUserIDMapping.get(article.getSource()));
                        tweetData.put(id,title);
                    }
                    res.put(topicID, tweetData);
                } else {
                    Map<Long,String> existing = res.get(topicID);
                    // it is a tweet transformed to an article
                    for (Article article : tweetsCluster) {
                        Long id = (tweetURLtoPostIDMapping.get(article.getSource()));
                        String title = (tweetURLtoUserIDMapping.get(article.getSource()));
                        existing.put(id,title);
                    }
                    res.put(topicID, existing);
                }
            }
        }
        // get tweets of the current topic id
        return res.get(tid);


    }


    private Map<String, Set<Long>> extractRelatedTweetIDs(
                Map<Topic, List<String>> relatedTweets,
                Map<String, Long> tweetURLtoPostIDMapping
    ) {
        Map<String, Set<Long>> res = new HashMap();
        for (Map.Entry<Topic, List<String>> entry : relatedTweets.entrySet()) {
            Topic tweetsCluster = entry.getKey();
            List<String> lClusterIDs = entry.getValue();

            for (String topicID : lClusterIDs) {
                if (!res.containsKey(topicID)) {
                    Set<Long> tweetIDs = new HashSet();
                    // it is a tweet transformed to an article
                    for (Article article : tweetsCluster) {
                        tweetIDs.add(tweetURLtoPostIDMapping.get(article.getSource()));
                    }
                    res.put(topicID, tweetIDs);
                } else {
                    Set<Long> existing = res.get(topicID);
                    // it is a tweet transformed to an article
                    for (Article article : tweetsCluster) {
                        existing.add(tweetURLtoPostIDMapping.get(article.getSource()));
                    }
                    res.put(topicID, existing);
                }
            }
        }
        return res;
    }

    public List<BDEArticle> loadArticlesAsDemo(long timestamp) {


        List<BDEArticle> res = new ArrayList();
        // npit edit : load all articles
        //Collection<Map<String, Object>> items = loadAllArticles(-1);
        Collection<Map<String, Object>> items = ((LocationCassandraRepository)this).loadArticles(timestamp);
        // wrap to Article instances
        for (Map<String, Object> eachItem : items) {
            String source_url = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
            String title = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName());
            String clean_text = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
            String feed_url = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName());
            long published = (long) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
            Date d = new Date();
            d.setTime(published);
            Set<String> place_literal = (Set<String>) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName());
            Map<String, String> places_to_polygons = new HashMap();
            for (String eachPlace : place_literal) {
                Map<String, Object> article = loadArticlePerPlace(eachPlace, source_url);
                places_to_polygons.put(eachPlace, (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName()));
            }
            res.add(new BDEArticle(source_url, title, clean_text, "Europe", feed_url, null, d, places_to_polygons));
        }
        return res;
    }


    public void  loadArticlesToCluster(long timestamp) {
        System.out.println(String.format("Loading articles using window :[%s] , max number of articles: [%d]",
                configuration.getDocumentRetrievalTimeWindow(),configuration.getMaxNumberOfArticles()));
        int maxNumber = configuration.getMaxNumberOfArticles();
        ArrayList<BDEArticle> articlesUnfiltered = new ArrayList<>();
        ArrayList<Long> crawled_dates = new ArrayList<>();
        // npit edit : load all articles
        //Collection<Map<String, Object>> items = loadAllArticles(5);
        Collection<Map<String, Object>> items = loadArticles(timestamp);
        // wrap to Article instances
        for (Map<String, Object> eachItem : items) {
            String source_url = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
            String title = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName());
            String clean_text = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
            String feed_url = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName());
            long published = (long) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
            long crawled = (long) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName());
            Date d = new Date();
            d.setTime(published);
            Set<String> place_literal = (Set<String>) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName());
            Map<String, String> places_to_polygons = new HashMap();
            for (String eachPlace : place_literal) {
                Map<String, Object> article = loadArticlePerPlace(eachPlace, source_url);
                places_to_polygons.put(eachPlace, (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName()));
            }
            articlesUnfiltered.add(new BDEArticle(source_url, title, clean_text, "Europe", feed_url, null, d, places_to_polygons));
            crawled_dates.add(crawled);

        }

        // get at most maxNumber articles, the most recent
        System.out.print("\tLimiting article set to the " + maxNumber +  " most recently crawled articles...");

        ArrayList<Long> crawledDatesSorted = (ArrayList) crawled_dates.clone();
        Collections.sort(crawledDatesSorted);
        Collections.reverse(crawledDatesSorted);
        articles = new ArrayList<>();
        // for each crawled timestamp
        for(int i=0;i<crawledDatesSorted.size();++i)
        {
            if (i >= maxNumber) break;

            // get index of the curr crawled date on the original list
            int articleIndex  = crawled_dates.indexOf(crawledDatesSorted.get(i));
            // get corresponding article, add it to the results
            articles.add(articlesUnfiltered.get(articleIndex));
            // limit max number
        }
        System.out.println("done.\n\tArticle loader returning " + articles.size() + " items.");
    }
    @Override
    public boolean good()
    {
        return status;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void clusterArticles()
    {
        System.out.println("clustering articles...");
        String clusteringMode = configuration.getClusteringMode();
        ArticlesPerCluster = null;

        if(clusteringMode.equals("base"))
        {
            System.out.println("Using base clusterer with cutoff threshold at [" + configuration.getCutOffThreshold() + "]");
            IArticleClusterer cl = new ParameterizedBaseArticleClusterer(articles, configuration.getCutOffThreshold());
            cl.calculateClusters();
            ArticlesPerCluster = (HashMap)cl.getArticlesPerCluster();
        }
        else if (clusteringMode.equals("mcl"))
        {
            Articles articles_collection = new Articles(articles);
            IClusterer cl = new MCLClusterer(articles_collection, configuration.getCutOffThreshold());
            cl.calculateClusters(articles_collection);
            ArticlesPerCluster = (HashMap) cl.getArticlesPerCluster();
        }
        else
        {
            System.out.println("Undefined clustering mode : " + clusteringMode);
            status = false;
            return;
        }
        System.out.println("Clsutering took  " + toc() + " sec. Clusters : ");
    }
    @Override
    public void loadTweetsToCluster(long timestamp) {

         tweets = new ArrayList();

        Collection<Map<String, Object>> items = loadTweets(timestamp);
        //Collection<Map<String, Object>> items = loadAllTweets(-1);

        // wrap to Article instances
        for (Map<String, Object> eachItem : items) {
            long post_id = (long) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName());
            String permalink = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.getColumnName());
            String tweet = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName());
            long created_at = (long) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName());
            String lang = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.getColumnName());
            // add extra entries to use twitter_results's constructor with the username field
            String user_name = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.getColumnName());
	    //System.out.println("created-at : " + created_at);
            tweets.add(new TwitterResult(post_id, 0l, permalink, tweet, new Timestamp(created_at).toString(), lang,new ArrayList<String>(),user_name,""));
        }
    }

    @Override
    public void calculateSummarization()
    {
        System.out.println("getting summaries");
        ISentenceSplitter splitter = null;
        try
        {
            splitter = new DefaultSentenceSplitter(configuration.getSentenceSplitterModelPath());
        }
        catch(NullPointerException ex)
        {
            System.err.println("Incorrect sentence splitter path.");
            status = false;
            return;
        }
        ISummarizer sum = new Summarizer(splitter);
        // get summaries
        summaries = sum.getSummaries(new HashSet(ArticlesPerCluster.values()));
    }


    @Override
    public void processTweets() {
        Collection<TwitterResult> tweets = getTweets();
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
        IArticleClusterer smClusterer = getSocialMediaClustererForTwitter(SocialMediaClusterer.Mode.NVS, cleanTweets);
        smClusterer.calculateClusters();
        Collection<Topic> tweetClusters = smClusterer.getArticlesPerCluster().values();

        System.out.println("Classifying tweets...");
        double min_assign_sim_threshold = configuration.getTwitterMappingSimilarityThreshold();
        double min_assign_titlesim_threshold = configuration.getTwitterMappingTitleSimilarityThreshold();

        System.out.println("Text similarity thesh:" + min_assign_sim_threshold );
        System.out.println("Text title similarity thesh:" + min_assign_titlesim_threshold );
        IClassifier smClassifier = getSocialMediaClassifierForTwitter(min_assign_sim_threshold, min_assign_titlesim_threshold,plainTextSummaries, tweetClusters, tsStemmer);
        RelatedTweets = smClassifier.getRelated();

        tweetURLtoPostIDMapping = getTweetClustersToIDsMappings(cleanTweets);
        tweetURLtoUserIDMapping = getTweetClustersToUsersMappings(cleanTweets);
    }

    @Override
    public Collection<TwitterResult> getTweets() {
        return tweets;
    }

    public Collection<Map<String, Object>> loadEvents(int limit) {
        Collection<Map<String, Object>> result = new ArrayList();
        // load items from cassandra
        Statement select;
        ResultSet results;
        try {
            select = QueryBuilder
                    .select()
                    .all()
                    .from(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                    .limit(limit);
            results = session.execute(select);
            for (Row row : results) {
                if (row != null) {
                    Map<String, Object> res = new HashMap();
                    String eventID = row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName());
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), eventID);
                    String title = row.getString(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName());
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName(), title);
                    String description = row.getString(Cassandra.Event.TBL_EVENTS.FLD_DESCRIPTION.getColumnName());
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_DESCRIPTION.getColumnName(), description);
                    String date_literal = row.getString(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName());
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName(), date_literal);
                    Map<String, String> place_mappings = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), String.class, String.class);
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), place_mappings);
                    Set<Long> tweetIDs = row.getSet(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(), Long.class);
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(), tweetIDs);
                    Set<String> sourceURLs = row.getSet(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), String.class);
                    res.put(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), sourceURLs);
                    // append to result
                    result.add(res);
                }
            }
        }catch(Exception ex){
            Logger.getLogger(ClusteringCassandraRepository.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        return result;
    }

    private Set<String> extractSourceURLs(Topic t) {
        Set<String> res = new HashSet();
        for (Article article : t) {
            res.add(article.getSource());
        }
        return res;
    }
    private Map<String,String> extractSourceURLTitlePairs(Topic t) {
        Map<String,String> res = new HashMap();
        for (Article article : t) {
            res.put(article.getSource(),article.getTitle());
        }
        return res;
    }

    @Override
    public void initialize()
    {
        status = true;

    }

    @Override
    public void printClusters()
    {
        for(String key : ArticlesPerCluster.keySet())
        {

            System.out.println(key);
            Topic t = ArticlesPerCluster.get(key);
            for(Article art : t)
            {
                System.out.println("\t" + art.getTitle());
            }
        }
        System.out.println("\n");
    }



    /**
     * Function to send events to strabon for storage and/or change detection
     */
    @Override
    public void remoteStoreEvents()
    {
        String strabonURL = configuration.getStrabonURL();
        System.out.println("Sending events to Strabon. url:[" + strabonURL + "]");
        // get all events, fields: id, title, date, placemappings
        // store each field of interest in an arraylist

        // perform the query
        Statement query = QueryBuilder
                .select(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName(),
                        Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName())
                .from(session.getLoggedKeyspace(),Cassandra.Event.Tables.EVENTS.getTableName());
        ResultSet results = session.execute(query);
        int count = 1;
        // for each event
        for(Row row : results)
        {
            // get the id
            String id = (row.getString(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName()));

            // place-mappings: If null, skip the processing
            Map<String,String> locpoly = row.getMap(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(),String.class,String.class);
            if (locpoly.isEmpty())
            {
                System.out.println("Skipping processing event " + count++ + ":" + id + " due to no assigned geometries." );
                continue;
            }

            // get the title
            String title = (row.getString(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName()));
            // get date
            String date = (row.getString(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName()));

            // reconstruct the entries in the format expected by strabon
            String payload="";
            try
            {
                payload = GeometryFormatTransformer.EventRowToStrabonJSON(id,title,date,locpoly);

            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            //System.out.println("payload is:<" + payload + ">"); // debugprint
            if(payload.isEmpty())
            {
                System.out.println("Empty payload, won't send anything.");
                return;
            }
            System.out.println("Sending event  [" + id + "].");

            String resp = gr.demokritos.iit.base.util.Utils.sendPOST(payload,strabonURL);
            Utils.checkResponse(resp);

        }

    }



    public Map<String,Topic> justCluster( HashMap<Tuple2<
            Tuple4<String, String, String, Long>,
            Tuple4<String, String, String, Long>>,Boolean> articlesPairsMapping)
    {
        // only the base clusterer is supported for now.
        ParameterizedBaseArticleClusterer clusterer = new ParameterizedBaseArticleClusterer(configuration.getCutOffThreshold());
        clusterer.onlyCluster(articlesPairsMapping);
        ArticlesPerCluster = (HashMap) clusterer.getArticlesPerCluster();

        return ArticlesPerCluster;
    }


    protected void setPlacesPerArticle(List<Tuple2<String,ArrayList<String>>> placesPerArticle)
    {
        // get the geometries for each place

    }

    protected Map<String,Map<String,String>> getGeometryInformationFromRepository(List<String> art)
    {
        return null;
    }

    private  Map<String, String> convertToPlainSummaries(Map<String, Summary> summaries) {
        Map<String, String> res = new HashMap();
        for (Map.Entry<String, Summary> entry : summaries.entrySet()) {
            Summary tmp = entry.getValue();
            res.put(entry.getKey(), toPlainText(tmp));
        }
        return res;
    }

    private  String toPlainText(Summary tmp) {
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



    public IArticleClusterer getSocialMediaClustererForTwitter(SocialMediaClusterer.Mode metrics_mode, CleanResultCollection<TwitterResult> tweets) {
        if (tweets == null) {
            return null;
        }
        SocialMediaClusterer.SocialMediaClustererBuilder builder
                = new SocialMediaClusterer.SocialMediaClustererBuilder()
                // set mode for clusterer
                .withMode(metrics_mode)
                .withDefaultSimilarityThreshold()
                .withDefaultSizeThreshold()
                .withTweets(tweets);
        // create clusterer
        return builder.build();
    }

    public IClassifier getSocialMediaClassifierForTwitter(
            Map<String, String> plainSummaries,
            Collection<Topic> clusters,
            IStemmer stemmer
    ) {
        // use Default Thresholds
        return new SocialMediaClassifier(plainSummaries, clusters, stemmer);
    }

    public IClassifier getSocialMediaClassifierForTwitter(
            double min_assign_sim_threshold,
            double min_assign_title_sim_threshold,
            Map<String, String> plainSummaries,
            Collection<Topic> clusters,
            IStemmer stemmer
    ){
        // use Default Thresholds
        return new SocialMediaClassifier(min_assign_sim_threshold, min_assign_title_sim_threshold, plainSummaries, clusters, stemmer);
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

    @Override
    public void changeDetectionTrigger()
    {
        System.out.println("Checking if change detection should be triggered for the newly generated events.");
        int threshold = configuration.getChangeDetectionThreshold();

        String url = configuration.getChangeDetectionURL();
        String username = configuration.getChangeDetectionUsername();
        String password = configuration.getChangeDetectionPassword();

        // title, descr, date, tweets, sources, id, placemappings


        for(int ev = 0; ev < SavedEvents.size(); ++ ev)
        {
            // event container , to return event for change detection
            // title, descr, date, tweets, sources, id, placemappings
            ArrayList<Object> currEvent = SavedEvents.get(ev);
            String eventid = (String) currEvent.get(5);
            // skip the ones with no location data
            if( ((Map<String,String>)currEvent.get(6)).isEmpty())
            {
                System.out.println("Skipping event " + eventid + " due to no assigned geometries");
                continue;
            }
            int numArticlesOfEvent = ((Map<String,String>)currEvent.get(4)).size();
            if( numArticlesOfEvent >= threshold)
            {
                System.out.println(String.format(
                        "\tTriggering change detection for event %s which has %d/%d sources",
                        eventid, numArticlesOfEvent, threshold));
                // get the smallest place (area)

                String eventDate = (String) currEvent.get(2);
                HashMap<String,String> places = (HashMap<String,String>)currEvent.get(6);
                double minArea = Double.MAX_VALUE;
                int minIdx=0;
                String minLoc="";
                for (String location : places.keySet())
                {
                    try {
                        double area = GeometryFormatTransformer.calculateArea(places.get(location));
                        if(area < minArea)
                        {
                            minArea = area;
                            minLoc = location;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if(Double.isInfinite(minArea))
                {
                    System.err.println("No geometries found for event " + eventid);
                    return;
                }
                String minGeom = places.get(minLoc);

                /* example format
        http://teleios4.di.uoa.gr:8080/changeDetection/changes/progress
            ?extent=POLYGON((10.513916015625 43.97996933797573,16.007080078125 43.97996933797573,16.007080078125 40.182020964319086,10.513916015625 40.182020964319086,10.513916015625 43.97996933797573))
            &reference_date=2016-03-10T10:57:53+0000
            &event_date=2016-03-16T10:57:53+0000
            &polarisations=[VH, VV]
            &username=efaki&password=testapp
         */

    //        System.err.println("HARDCODING mingeom");
    //        System.out.println("HARDCODED");
    //        minGeom="POLYGON ((13.1680793762208 42.6952133178712, 13.4106550216675 42.6952133178712, 13.4106550216675 42.5705032348633, 13.1680793762208 42.5705032348633, 13.1680793762208 42.6952133178712))";

                String payload = null;
                try {
                    payload = url + "?extent=" + java.net.URLEncoder.encode(minGeom,"UTF-8") +
                            "&reference_date=" + java.net.URLEncoder.encode(eventDate,"UTF-8") +
                            "&event_date=" + java.net.URLEncoder.encode(eventDate,"UTF-8") +
                            "&polarisations=" + java.net.URLEncoder.encode("[VH, VV]","UTF-8") +
                            "&username=" + username +
                            "&password=" + password;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                System.out.println("Sending to [" + payload + "]");
                // send that min area
                String resp = null;
                try {
                    resp = Utils.sendGET(payload);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(Utils.checkResponse(resp))
                    System.err.println("Change detection trigger for event " + eventid + " failed.");



            }
        }





        // event container , to return event for change detection
        // title, descr, date, tweets, sources, id, placemappings

        // maybe we dont need the event argument ( or event returning the whole events from the saveEvents
        // can be just boolean, if we need to only trigger the remote with no arguments

    }

}
