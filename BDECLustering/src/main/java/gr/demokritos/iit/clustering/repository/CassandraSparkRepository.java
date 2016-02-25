/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.repository;

import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.CassandraRow;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.clustering.util.CassandraArticleRowToTuple4RDD;
import gr.demokritos.iit.clustering.util.FilterByAfterTimeStamp;
import java.util.Calendar;
import java.util.List;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple4;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraSparkRepository {

    private final SparkContextJavaFunctions scjf;
    private final String keyspace;

    public CassandraSparkRepository(SparkContext sc, String keyspaceArg) {
        this.scjf = CassandraJavaUtil.javaFunctions(sc);
        this.keyspace = keyspaceArg;
    }

    public long getLatestTimestamp(String table) {
//        CassandraJavaRDD<CassandraRow> limit = scjf.cassandraTable(keyspace, table).select("last_parsed").limit(1l);
//        List<CassandraRow> collect = limit.collect();
//        return collect.get(0).getLong("last_parsed");
        // dummy.
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) - 7);
        return now.getTimeInMillis();
    }

    /**
     * load articles in a tuple4 format <br> CAUTION: clean_text contains title,
     * we need title for allocating it in clusters
     *
     * @param timestamp from epoch
     * @return <entry_url, title, clean_text, timestamp>
     */
    public JavaRDD<Tuple4<String, String, String, Long>> loadArticlesPublishedLaterThan(long timestamp) {
        JavaRDD<CassandraRow> filter = scjf
                .cassandraTable(keyspace, Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
                .select(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(),
                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName())
                //                .limit(20l);
                .filter(new FilterByAfterTimeStamp(timestamp));
        JavaRDD<Tuple4<String, String, String, Long>> extracted = filter.map(new CassandraArticleRowToTuple4RDD(
                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(),
                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(),
                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName())
        );
        return extracted;
    }
}
