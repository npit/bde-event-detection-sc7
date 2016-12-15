package gr.demokritos.iit.clustering.util;

import gr.demokritos.iit.clustering.model.IdentifiableDocumentWordGraph;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple4;

/**
 * Created by npittaras on 3/10/2016.
 */
public class ArticlePairGraphCalculator implements Function<Tuple2<Tuple4<String,String,String, Long>,Tuple4<String,String,String, Long>>,
        Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph>>
{
    // <entry_url, title, clean_text, timestamp>, <entry_url, title, clean_text, timestamp>
    public ArticlePairGraphCalculator()
    {
    }
    public Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph> call(Tuple2<Tuple4<String,String,String, Long>,Tuple4<String,String,String, Long>> articleTuple)
    {
        Tuple4<String,String,String, Long> art1 = articleTuple._1();
        Tuple4<String,String,String, Long> art2 = articleTuple._2();

        return new Tuple2<>(StructUtils.articleTupleToDWG(art1), StructUtils.articleTupleToDWG(art2));

    }

}
