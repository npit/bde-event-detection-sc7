package gr.demokritos.iit.clustering.util;

import com.datastax.spark.connector.japi.CassandraRow;
import org.apache.spark.api.java.function.Function;
import scala.Tuple4;
import scala.Tuple5;

/**
 * Created by nik on 12/2/16.
 */
public class SortCassandraRowsByCrawledDate implements Function<CassandraRow, Long > {
    String crawledDateColumnName;
    public SortCassandraRowsByCrawledDate(String crawledDateColumnName)
    {

        this.crawledDateColumnName = crawledDateColumnName;
    }

    @Override
    public Long call(CassandraRow v1) throws Exception {
        return v1.getLong(crawledDateColumnName);
    }
}
