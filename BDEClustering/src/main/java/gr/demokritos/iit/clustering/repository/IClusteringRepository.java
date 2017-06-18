package gr.demokritos.iit.clustering.repository;

import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.newsum.server.model.structures.Topic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by npittaras on 2/12/2016.
 */
public interface IClusteringRepository {

    void initialize();
    void remoteStoreEvents();
    void localStoreEvents();
    void changeDetectionTrigger();

    void loadArticlesToCluster();
    void clusterArticles();

    boolean good();
    void destroy();
    void printClusters();
    void printArticles();

    void calculateSummarization();

    void loadTweetsToCluster();
    void processTweets();
    Map<String,Map<String,String>> getImages(Map<String, Map<String, String>> placemappings, HashMap<String, Topic> ArticlesPerCluster);


    Collection<TwitterResult> getTweets();
}
