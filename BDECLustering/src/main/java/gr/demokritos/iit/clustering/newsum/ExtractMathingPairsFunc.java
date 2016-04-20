package gr.demokritos.iit.clustering.newsum;

import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple4;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class ExtractMathingPairsFunc implements Function<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>, Boolean> {

    private final SparkContext sc;

    public ExtractMathingPairsFunc(SparkContext sc, SimilarityMode mode, double simCutOff) {
        this.sc = sc;
    }

    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    @Override
    public Boolean call(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> v1) throws Exception {

        // get text from first item
        StringEntity ent1 = new StringEntity();
        // concatenate title and clean text together
        ent1.setString(sc, new StringBuilder().append(v1._1()._2()).append(" ").append(v1._1()._3()).toString());
        // get text from second item
        StringEntity ent2 = new StringEntity();
        // concatenate title and clean text together
        ent1.setString(sc, new StringBuilder().append(v1._2()._2()).append(" ").append(v1._2()._3()).toString());

        // TODO: get similarities and return based on cut off threshold

        // just compile
        return false;
    }
}
