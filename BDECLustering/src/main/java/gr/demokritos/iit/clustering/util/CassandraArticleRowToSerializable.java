package gr.demokritos.iit.clustering.util;

import com.datastax.spark.connector.japi.CassandraRow;
import gr.demokritos.iit.clustering.parallelngg.graph.NGramGraphCreator;
import gr.demokritos.iit.clustering.parallelngg.structs.StringEntity;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Graph;
import scala.Tuple4;
import scala.Tuple5;
import java.lang.Object;

/**
 * Created by npit on 8/23/16.
 */
public class CassandraArticleRowToSerializable implements Function<CassandraRow, Tuple5<String, String, String, Long,Graph<String,Object>>> {


        private final String title;
        private final String text;
        private final String source;
        private final String date;
        Graph<String, Object> graph;

        public CassandraArticleRowToSerializable(String string_row_1_field, String string_row_2_field, String string_row_3_field, String long_row_4_field, int numPartitions,SparkContext sc) {
            // function gets the 4 article fields
            // title, text, source, date
            // for each article, need: the 4 string fields
            // (string entity=> NGramGraphCreator)
            this.title = string_row_1_field;
            this.text = string_row_2_field;
            this.source = string_row_3_field;
            this.date = long_row_4_field;
            StringEntity ent1 = new StringEntity();
            //ent1.setString(new StringBuilder().append(title).append(" ").append(text).toString());  // no SC!
            ent1.setString(sc, new StringBuilder().append(title).append(" ").append(text).toString());
            NGramGraphCreator ngc = new NGramGraphCreator(sc, numPartitions, 3, 3);
            graph = ngc.getGraph(ent1);
        }

        @Override
        public Tuple5<String, String, String, Long, Graph<String,Object>> call(CassandraRow arg0) throws Exception {

            // input is a cassandra row, can only get primitive (CQLSH) values from it.
            return new Tuple5(
                    arg0.getString(title), arg0.getString(text), arg0.getString(source), arg0.getLong(date), graph
            );
        }

}
