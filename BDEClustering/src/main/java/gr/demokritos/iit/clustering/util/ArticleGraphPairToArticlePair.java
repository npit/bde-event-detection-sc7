package gr.demokritos.iit.clustering.util;

import gr.demokritos.iit.clustering.model.IdentifiableDocumentWordGraph;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple4;

/**
 * Created by nik on 12/17/16.
 */
public class ArticleGraphPairToArticlePair implements Function<Tuple2<
        Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>,
        Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>
        >
        ,

        Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>
        > {

    @Override
    public Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> call(
            Tuple2<
                    Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>,
                    Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>> arg) throws Exception {

        return new Tuple2<>(
                arg._1()._1() , arg._2()._1()
        );

    }
}
