package gr.demokritos.iit.clustering.util;

import com.datastax.spark.connector.japi.CassandraRow;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by npittaras on 2/12/2016.
 */
public class CassandraRowToPlaces implements Function<CassandraRow, Tuple2<String,ArrayList<String>>> {

    private String placesColumn;
    private String urlColumn;
    public CassandraRowToPlaces(String placesColumn,String urlColumn)

    {
        this.urlColumn = urlColumn;
        this.placesColumn = placesColumn;
    }
    @Override
    public Tuple2<String,ArrayList<String>> call(CassandraRow v1) throws Exception {
        return new Tuple2<String,ArrayList<String>>(v1.getString(urlColumn),(ArrayList) v1.getList(placesColumn));
    }
}
