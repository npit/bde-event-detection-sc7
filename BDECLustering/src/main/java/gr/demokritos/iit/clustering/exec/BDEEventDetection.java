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

import gr.demokritos.iit.clustering.config.BDESpark;
import gr.demokritos.iit.clustering.config.BDESparkConf;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingPairsFunc;
import gr.demokritos.iit.clustering.newsum.IClusterer;
import gr.demokritos.iit.clustering.newsum.NSClusterer;
import gr.demokritos.iit.clustering.repository.CassandraSparkRepository;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;
import scala.Tuple4;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDEEventDetection {

    //    private final SparkContext sc;
    private final BDESpark sp;

    public BDEEventDetection(BDESpark bdes) {
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

        // init configuration
        ISparkConf conf = new BDESparkConf(args[0]);
        // init sparkConf (holds the spark context object)
        BDESpark bdes = new BDESpark(conf);
        // instantiate us
        BDEEventDetection bdedet = new BDEEventDetection(bdes);
        // keep context to pass around
        SparkContext sc = bdedet.getContext();
        // get the spark repository class
        CassandraSparkRepository repo = new CassandraSparkRepository(sc, conf);
        // get a timestamp : TODO: FIXME
        long timestamp = repo.getLatestTimestamp("event_detection_log"); // TODO: add table(?) or use parameter days_back.
        System.out.println(new Date(timestamp).toString());
        System.out.println("LOADING ARTICLES");
        // load batch. The quadruple represents <entry_url, title, clean_text, timestamp>
        // entry URL is supposed to be the unique identifier of an article (though for reuters many articles with same body
        // are republished under different URLs)
        JavaRDD<Tuple4<String, String, String, Long>> RDDbatch = repo.loadArticlesPublishedLaterThan(timestamp);

//        // instantiate a clusterer
//        IClusterer clusterer = new NSClusterer(sc, conf.getSimilarityMode(), conf.getCutOffThreshold(), conf.getNumPartitions());
//
//        // TODO: we should return the clusters (e.g. a map RDD of ID, List<Tuple4<>>)
//        clusterer.calculateClusters(RDDbatch);

        // create pairs
        System.out.println("EXTRACTING PAIRS");
        // get pairs of articles
        JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs
                = RDDbatch.cartesian(RDDbatch).filter(new DocumentPairGenerationFilterFunction());
        // debug
        StructUtils.printArticlePairs(RDDPairs, 5);
        // get matching mapping

        // TODO: use flatMap?? we want for the full pairs rdd, each item mapped to a boolean value.
        JavaRDD<Boolean> map = RDDPairs.map(new ExtractMatchingPairsFunc(sc, conf.getSimilarityMode(), conf.getCutOffThreshold(), conf.getNumPartitions()));
        // generate clusters

        // TODO: change method signature: return smth (not void)

        // get matching mappings


        // generate clusters

        // save clusters
    }
}
