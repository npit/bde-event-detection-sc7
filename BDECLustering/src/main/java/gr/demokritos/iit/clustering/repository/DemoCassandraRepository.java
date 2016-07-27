package gr.demokritos.iit.clustering.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.jinsect.structs.Pair;
import gr.demokritos.iit.location.repository.LocationCassandraRepository;
import org.apache.commons.collections.map.HashedMap;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static gr.demokritos.iit.base.conf.IBaseConf.DATE_FORMAT_ISO_8601;
import static gr.demokritos.iit.base.conf.IBaseConf.TIMEZONE_ID_UTC;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class DemoCassandraRepository extends LocationCassandraRepository {

    public DemoCassandraRepository(Session session) {
        super(session);
    }

    // use this class to save topics for DEMO
    // event_size_cuttof: do not save topics with size less than this value
    public void saveEvents(Map<String, Topic> topics,
                           Map<String, Summary> summaries,
                           Map<Topic, List<String>> relatedTweets,
                           Map<String, Map<String, String>> place_mappings,
                           Map<String, Long> tweetURLtoPostIDMapping,
                           Map<String, String> tweetURLtoUserIDMapping,
                           int event_size_cuttof

    ) {
        for (Map.Entry<String, Topic> entry  : topics.entrySet()) {
            String id = entry.getKey();
            Topic t = entry.getValue();
            if (t.size() >= event_size_cuttof) {
                saveEvent(id, t, summaries.get(id), relatedTweets, place_mappings, tweetURLtoPostIDMapping,tweetURLtoUserIDMapping);
            }
        }
    }

    public void saveEvent(String topicID,
                          Topic t,
                          Summary s,
                          Map<Topic, List<String>> relatedTweets,
                          Map<String, Map<String, String>> places_polygons_per_id,
                          Map<String, Long> tweetURLtoPostIDMapping,
                          Map<String, String> tweetURLtoUserIDMapping

                          ) {

        String title = t.getTitle();
        System.out.println(String.format("saving event id: %s with title: %s", topicID, title));
        // set top sentence as description
        String description = s.asSummaryData().getAllSentences().get(0).getSnippet();
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
        if(tweetIDsUsers == null) System.out.println("Null tweet ids");
        // news : updated to extract URL + title pairs
        //Set<String> topicSourceURLs = extractSourceURLs(t);
        Map<String,String> topicSourceURL_Titles = extractSourceURLTitlePairs(t);
        // update events
        Statement upsert = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.Event.Tables.EVENTS.getTableName())
                .with(set(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName(), title))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_DESCRIPTION.getColumnName(), description))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName(), sUTCEventDate))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName(), place_mappings))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName(),
                        tweetIDsUsers == null ? Collections.EMPTY_SET : tweetIDsUsers))
               // .and(set(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), topicSourceURLs))
                .and(set(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName(), topicSourceURL_Titles))
                .where(eq(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName(), topicID));
        System.out.println(upsert.toString());
        session.execute(upsert);

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
            session.execute(upsert);
        }
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
        Collection<Map<String, Object>> items = loadAllArticles(-1);
        //Collection<Map<String, Object>> items = loadArticles(timestamp);
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

    public Collection<TwitterResult> loadTweetsAsDemo(long timestamp) {
        Collection<TwitterResult> res = new ArrayList();

       // Collection<Map<String, Object>> items = loadTweets(timestamp);
        Collection<Map<String, Object>> items = loadAllTweets(-1);

        // wrap to Article instances
        for (Map<String, Object> eachItem : items) {
            long post_id = (long) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName());
            String permalink = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_PERMALINK.getColumnName());
            String tweet = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName());
            long created_at = (long) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName());
            String lang = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_LANGUAGE.getColumnName());
            // add extra entries to use twitter_results's constructor with the username field
            String user_name = (String) eachItem.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_ACCOUNT_NAME.getColumnName());
            res.add(new TwitterResult(post_id, 0l, permalink, tweet, Long.toString(created_at), lang,new ArrayList<String>(),user_name,""));
        }
        return res;
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
            Logger.getLogger(DemoCassandraRepository.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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
}
