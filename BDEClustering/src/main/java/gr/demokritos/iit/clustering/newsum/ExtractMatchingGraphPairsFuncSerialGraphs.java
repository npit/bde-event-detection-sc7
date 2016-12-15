package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.model.IdentifiableDocumentWordGraph;
import gr.demokritos.iit.clustering.parallelngg.graph.GraphSimilarityComparator;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraph;
import gr.demokritos.iit.clustering.parallelngg.graph.WordNGramGraph;
import gr.demokritos.iit.clustering.parallelngg.traits.DocumentGraph;
import gr.demokritos.iit.clustering.parallelngg.traits.Similarity;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple4;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class ExtractMatchingGraphPairsFuncSerialGraphs implements Function<Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph>, Boolean> {


    public enum NGRAM_MODE
    {
        WORD("WORD"), CHAR("search"),;
        private String type;

        private NGRAM_MODE(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }
    private final SimilarityMode mode;
    private final double simCutOff;
    private NGRAM_MODE NGramMode;



    public ExtractMatchingGraphPairsFuncSerialGraphs(SimilarityMode modeArg, double simCutOffArg, String NGramMode) {
        this.mode = modeArg;
        this.simCutOff = simCutOffArg;
        if(NGramMode.equals("word"))
            this.NGramMode = NGRAM_MODE.WORD;
        else
            this.NGramMode = NGRAM_MODE.CHAR;

    }
    public ExtractMatchingGraphPairsFuncSerialGraphs(SimilarityMode modeArg, double simCutOffArg) {
        this.mode = modeArg;
        this.simCutOff = simCutOffArg;
        this.NGramMode = null;
    }
    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    @Override
    public Boolean call(Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph> arg) throws Exception {



        double similarityValue  = this.compareGraphsJInsect(arg._1(),arg._2());
        //double similarityValue  = this.compareGraphsParallelNGG(arg._1()._2(),arg._1()._3(),arg._2()._2(),arg._2()._3());



        return similarityValue >= this.simCutOff;
    }

    // calculate graph similarity using parallelNGG
    private double compareGraphsParallelNGG(String title1,String text1, String title2, String text2)
    {
        DocumentGraph g1 = null, g2 = null;

        int n=1;
        int win=3;
        switch (NGramMode)
        {
            case WORD:
                g1 = new WordNGramGraph(n,win);
                g2 = new WordNGramGraph(n,win);

                break;
            case CHAR:
                g1 = new NGramGraph(n,win);
                g2 = new NGramGraph(n,win);


                break;
        }
        g1.fromString(title1+" "+text1);

        g2.fromString(title2+" "+text2);

        GraphSimilarityComparator gsc = new GraphSimilarityComparator();
        Similarity gs = gsc.getSimilarity(g1,g2);
        gsc.getSimilarity(g1,g2);

//        System.out.printf("PNGG: NVS: %2.9f VS: %2.9f CS: %2.9f SS: %2.9f",
//                (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get() ,
//                (double) gs.getSimilarityComponents().get(SimilarityMode.VS.getGraphSimilarity()).get() ,
//                (double) gs.getSimilarityComponents().get(SimilarityMode.CS.getGraphSimilarity()).get() ,
//                (double) gs.getSimilarityComponents().get(SimilarityMode.SS.getGraphSimilarity()).get()
//        );
//        System.out.println(" ||| Article pair: t1: ["  + title1+ "]  and t2: [" +title2+ "] : ");


        // decide based on mode
        switch (mode) {
            case NVS:
                //double val = (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get();
                return (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get();
            case VS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.VS.getGraphSimilarity()).get();
            case CS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.CS.getGraphSimilarity()).get();
            case SS:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.SS.getGraphSimilarity()).get();
            default:
                return (double) gs.getSimilarityComponents().get(SimilarityMode.NVS.getGraphSimilarity()).get();
        }
    }

    // calculate graph similarity using JInsect
    private double compareGraphsJInsect(DocumentWordGraph jig1, DocumentWordGraph jig2)
    {
        GraphSimilarity gs = null;


        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        gs = ngc.getSimilarityBetween(jig1, jig2);

        double NVS = gs.SizeSimilarity == 0.0D?0.0D:gs.ValueSimilarity / gs.SizeSimilarity;
//
//        System.out.printf("JINS: NVS: %2.9f VS: %2.9f CS: %2.9f SS: %2.9f",
//                NVS,
//                gs.ValueSimilarity,
//                gs.ContainmentSimilarity,
//                gs.SizeSimilarity
//        );
//        System.out.println(" ||| Article pair: t1: ["  + title1+ "]  and t2: [" +title2+ "] : ");

        switch (mode) {
            case NVS:
                return NVS;
            case VS:
                return gs.ValueSimilarity;
            case CS:
                return gs.ContainmentSimilarity;
            case SS:
                return gs.SizeSimilarity;
            default:
                return NVS;

        }

    }
}
