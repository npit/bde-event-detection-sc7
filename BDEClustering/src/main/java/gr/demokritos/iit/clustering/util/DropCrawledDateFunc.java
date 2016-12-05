package gr.demokritos.iit.clustering.util;

import org.apache.spark.api.java.function.Function;
import scala.Tuple4;
import scala.Tuple5;

/**
 * Created by nik on 12/2/16.
 */
public class DropCrawledDateFunc implements Function<Tuple5<String,String,String,Long,Long>, Tuple4<String,String,String,Long>> {
    @Override
    public Tuple4<String, String, String, Long> call(Tuple5<String, String, String, Long, Long> v1) throws Exception {
        return new Tuple4<>(v1._1(), v1._2(), v1._3(), v1._4());
    }
}
