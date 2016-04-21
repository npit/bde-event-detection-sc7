package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple4;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class NSClusterer implements IClusterer {

    private final SparkContext sc;
    private final SimilarityMode mode;
    private final double simCutOff;
    private final int numPartitions;

    public NSClusterer(SparkContext scArg, SimilarityMode mode, double simCutOff, int numPartitions) {
        this.sc = scArg;
        this.mode = mode;
        this.simCutOff = simCutOff;
        this.numPartitions = numPartitions;
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
        JavaRDD<Boolean> map = RDDPairs.map(new ExtractMatchingPairsFunc(sc, mode, simCutOff, numPartitions));
        // generate clusters

        // TODO: change method signature: return smth (not void)


    }


}
