package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple4;

/**
 * Created by gkioumis on 3/11/16.
 */
public class NSClusterer implements IClusterer {

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
        // TODO:
//        RDDPairs.map()
        // generate clusters

        // TODO: change method signature: return smth (not void)


    }


}
