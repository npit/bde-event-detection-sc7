package gr.demokritos.iit.clustering.util;
import com.datastax.spark.connector.japi.CassandraRow;

import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

/**
 * Created by nik on 12/4/16.
 */
public class FirstPairTuple5PairElement implements Function<Tuple2<CassandraRow, Long>,CassandraRow> {

    @Override
    public CassandraRow call(Tuple2<CassandraRow, Long> v1) throws Exception {
        return v1._1();    }
}
