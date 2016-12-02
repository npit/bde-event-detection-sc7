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

import java.util.HashMap;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class ExtractMatchingGraphPairsFunc implements Function<Tuple2<Graph<String, Object>, Graph<String, Object>>, Boolean> {


    private final SimilarityMode mode;
    private final double simCutOff;
    private final int numPartitions;

    public ExtractMatchingGraphPairsFunc(SimilarityMode modeArg, double simCutOffArg, int numPart) {
        this.mode = modeArg;
        this.simCutOff = simCutOffArg;
        this.numPartitions = numPart;
    }

    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    @Override
    public Boolean call(Tuple2<Graph<String, Object>, Graph<String, Object>> v1) throws Exception {


        Graph<String, Object> ngg1 = v1._1();
        Graph<String, Object> ngg2 = v1._2();
//        if (ngg1 == null) System.out.println("graph#1 is null");
//        if (ngg2 == null) System.out.println("graph#2 is null");
//        if (ngg1.edges() == null) System.out.println("graph#1 edge is null");
//        if (ngg2.edges() == null) System.out.println("graph#2 edge is null");

        // get graph similarity
        GraphSimilarityCalculator gsc = new GraphSimilarityCalculator();
        Similarity gs = gsc.getSimilarity(ngg1, ngg2);

        scala.collection.immutable.Map<String,Object> M =  gs.getSimilarityComponents();
        System.out.println("Similarity: " + (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get() + " , cutoff "  + simCutOff);
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
