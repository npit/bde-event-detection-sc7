package gr.demokritos.iit.clustering.newsum;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple4;

public interface IClusterer {

    /**
     *
     * @param articles expects an RDD of <URL, title, text, timestamp>
     */
    void calculateClusters(JavaRDD<Tuple4<String, String, String, Long>> articles);

}
