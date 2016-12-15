package gr.demokritos.iit.clustering.util;

import gr.demokritos.iit.clustering.model.IdentifiableDocumentWordGraph;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Graph;
import scala.Tuple4;

/**
 * Created by npittaras on 3/10/2016.
 */
public class ArticleGraphCalculator implements Function<Tuple4<String,String,String, Long>,IdentifiableDocumentWordGraph>
{
    private static transient SparkContext sc;
    private int numPartitions;
    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    public ArticleGraphCalculator()
    {

    }
    public IdentifiableDocumentWordGraph call(Tuple4<String,String,String, Long> articleTuple)
    {
        return  StructUtils.articleTupleToDWG(articleTuple);
    }

}
