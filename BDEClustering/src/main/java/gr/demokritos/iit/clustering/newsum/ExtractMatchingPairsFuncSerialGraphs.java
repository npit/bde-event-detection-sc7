package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.parallelngg.graph.GraphSimilarityCalculator;
import gr.demokritos.iit.clustering.parallelngg.graph.GraphSimilarityComparator;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraph;

import gr.demokritos.iit.clustering.parallelngg.traits.Similarity;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Graph;
import scala.Tuple2;
import scala.Tuple4;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class ExtractMatchingPairsFuncSerialGraphs implements Function<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>, Boolean> {


    private final SimilarityMode mode;
    private final double simCutOff;

    public ExtractMatchingPairsFuncSerialGraphs(SimilarityMode modeArg, double simCutOffArg) {
        this.mode = modeArg;
        this.simCutOff = simCutOffArg;
    }

    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    @Override
    public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> v1) throws Exception {

        // spark-context
        // get text from first item

        String article1Representation = new StringBuilder().append(v1._1()._2()).append(" ").append(v1._1()._3()).toString();
        String article2Representation = new StringBuilder().append(v1._2()._2()).append(" ").append(v1._2()._3()).toString();
        NGramGraph g1 = new NGramGraph(1,1);
        g1.fromString(article1Representation);

        NGramGraph g2 = new NGramGraph(1,1);
        g2.fromString(article2Representation);
        GraphSimilarityComparator gsc = new GraphSimilarityComparator();
        Similarity gs = gsc.getSimilarity(g1,g2);

        //System.out.println("Similarity: " + (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get() + " , cutoff "  + simCutOff);


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
