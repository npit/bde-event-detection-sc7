package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.parallelngg.graph.GraphSimilarityCalculator;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import gr.demokritos.iit.clustering.parallelngg.traits.Similarity;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Graph;
import scala.Tuple2;
import scala.Tuple4;

import java.io.Serializable;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class ExtractMatchingPairsFunc implements Function<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>, Boolean> {

    private final transient SparkContext sc;
    private final SimilarityMode mode;
    private final double simCutOff;
    private final int numPartitions;

    public ExtractMatchingPairsFunc(SparkContext sc, SimilarityMode modeArg, double simCutOffArg, int numPart) {
        this.sc = sc;
        this.mode = modeArg;
        this.simCutOff = simCutOffArg;
        this.numPartitions = numPart;
    }

    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    @Override
    public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> v1) throws Exception {
        // spark-context
        // get text from first item
        StringEntity ent1 = new StringEntity();
        // concatenate title and clean text together
        ent1.setString(sc, new StringBuilder().append(v1._1()._2()).append(" ").append(v1._1()._3()).toString());
        // get text from second item
        StringEntity ent2 = new StringEntity();
        // concatenate title and clean text together
        ent1.setString(sc, new StringBuilder().append(v1._2()._2()).append(" ").append(v1._2()._3()).toString());

        // create graphs for each document
        NGramGraphCreator ngc1 = new NGramGraphCreator(sc, numPartitions, 3, 3);
        Graph<String, Object> ngg1 = ngc1.getGraph(ent1);
        NGramGraphCreator ngc2 = new NGramGraphCreator(sc, numPartitions, 3, 3);
        Graph<String, Object> ngg2 = ngc2.getGraph(ent2);
        // get graph similarity
        GraphSimilarityCalculator gsc = new GraphSimilarityCalculator();
        Similarity gs = gsc.getSimilarity(ngg1, ngg2);

        // decide based on mode
        switch (mode) {
            case NVS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get() >= simCutOff;
            case VS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.VS.getGraphSimilarity()).get() >= simCutOff;
            case CS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.CS.getGraphSimilarity()).get() >= simCutOff;
            case SS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.SS.getGraphSimilarity()).get() >= simCutOff;
            default:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get() >= simCutOff;
        }
    }
}
