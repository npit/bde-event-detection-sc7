package gr.demokritos.iit.clustering.util;

import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Graph;
import scala.Tuple4;

/**
 * Created by npittaras on 3/10/2016.
 */
public class ArticleGraphCalculator implements Function<Tuple4<String,String,String, Long>, Graph<String, Object>>
{
    private static transient SparkContext sc;
    private int numPartitions;
    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    public ArticleGraphCalculator(SparkContext sc, int numPartitions)
    {
        this.sc = sc;
        this.numPartitions = numPartitions;
    }
    public Graph<String, Object> call(Tuple4<String,String,String, Long> articleTuple)
    {
        StringEntity ent1 = new StringEntity();
        String title = articleTuple._2();
        String text = articleTuple._3();
        // concatenate title and clean text together
        ent1.setString(sc, new StringBuilder().append(title).append(" ").append(text).toString());
        NGramGraphCreator ngc = new NGramGraphCreator(sc, numPartitions, 3, 3);
        Graph<String, Object> graphobj = ngc.getGraph(ent1);

        return graphobj;
//        return graphobj;
    }

}
