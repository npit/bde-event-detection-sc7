package gr.demokritos.iit.clustering.repository;

import org.scify.asset.server.model.structures.social.TwitterResult;

import java.util.Collection;

/**
 * Created by npittaras on 2/12/2016.
 */
public interface IClusteringRepository {

    void initialize();
    void remoteStoreEvents();
    void localStoreEvents();
    void changeDetectionTrigger();

    void loadArticlesToCluster(long timestamp);
    void clusterArticles();

    boolean good();
    void destroy();
    void printClusters();
    void printArticles();

    void calculateSummarization();

    void loadTweetsToCluster(long timestamp);
    void processTweets();
    Collection<TwitterResult> getTweets();
}
