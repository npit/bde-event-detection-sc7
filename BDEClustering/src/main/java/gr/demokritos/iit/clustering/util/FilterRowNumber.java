package gr.demokritos.iit.clustering.util;

import org.apache.spark.api.java.function.Function;
import scala.Tuple2;
import scala.Tuple5;
import com.datastax.spark.connector.japi.CassandraRow;

/**
 * Created by nik on 12/4/16.
 */
public class FilterRowNumber implements Function<Tuple2<CassandraRow,Long>,Boolean> {
    int limit;
    public FilterRowNumber(int limit)
    {
        this.limit = limit;
    }
    @Override
    public Boolean call(Tuple2<CassandraRow, Long> v1) throws Exception {
        return(v1._2() <= limit);
    }
}
