package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.scify.newsum.server.clustering.BaseArticleClusterer;
import org.scify.newsum.server.model.datacollections.Articles;
import org.scify.newsum.server.model.structures.Topic;
import scala.Tuple2;
import scala.Tuple4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class NSClusterer implements IClusterer {

    private final SparkContext sc;
    private final SimilarityMode mode;
    private final double simCutOff;
    private final int numPartitions;
    private List<Topic> Clusters;

    protected Map<String, Topic> ArticlesPerCluster;
    protected Map<String, String> ClustersPerArticle;


    public NSClusterer(SparkContext scArg, SimilarityMode mode, double simCutOff, int numPartitions) {
        this.sc = scArg;
        this.mode = mode;
        this.simCutOff = simCutOff;
        this.numPartitions = numPartitions;
    }

    private class Article
    {


    }
    @Override
    public void calculateClusters(JavaRDD<Tuple4<String, String, String, Long>> articles) {

        // create pairs
        System.out.println("EXTRACTING PAIRS");
        // get pairs of articles
        JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs
                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());
        // debug
        StructUtils.printArticlePairs(RDDPairs, 5);
        // get matching mapping

        // TODO: use flatMap?? we want for the full pairs rdd, each item mapped to a boolean value.
        JavaRDD<Boolean> matchesrdd = RDDPairs.map(new ExtractMatchingPairsFunc(sc, mode, simCutOff, numPartitions));
        List<Boolean> matches = matchesrdd.collect();
        // generate clusters
        // baseArticleClusterer shuts down executors. Should we collect the data and do that here too?

        // loop on the pairs
        ArticlesPerCluster = new HashMap<String,Topic>();
        ClustersPerArticle = new HashMap<String,String>();
        /*
        int count = -1;
        boolean matchValue;
        for(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> pair : RDDPairs.collect())
        {
            ++ count; // pair index
            matchValue = matches.get(count);
            String clusterID;
            Topic newTopic;
            if(matchValue) // if the article pair matches
            {
                // if both exist in a cluster, merge the clusters
                if(true)
                {

                    continue;
                }

                // one or more articles is not in any cluster
                if(true) // if first is not in any cluster, create one and enter it
                {

                }
                // if b is in a cluster
            }
            else  // if they do not match
            {


            }

        }

        */

        // create triple-tuples <pair1, pair2, matchOrNot> ?

        // TODO: change method signature: return smth (not void)


    }

    @Override
    public void calculateClusters(Articles articles) {

    }

    @Override
    public Map<String, Topic> getArticlesPerCluster() {
        return null;
    }


}
