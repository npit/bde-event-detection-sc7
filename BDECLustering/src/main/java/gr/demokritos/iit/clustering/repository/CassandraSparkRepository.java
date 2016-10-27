/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.clustering.repository;

import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.CassandraRow;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.clustering.config.ISparkConf;
import gr.demokritos.iit.clustering.util.CassandraArticleRowToTuple4RDD;
import gr.demokritos.iit.clustering.util.FilterByAfterTimeStamp;
import java.util.Calendar;
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
    private final SparkContext sc;
    private final int numDaysBatch;

    public CassandraSparkRepository(SparkContext scArg, ISparkConf conf) {
        this.scjf = CassandraJavaUtil.javaFunctions(scArg);
        this.keyspace = conf.getCassandraKeyspace();
        this.sc = scArg;
        this.numDaysBatch = conf.getNumDaysBatch();
    }

    public long getLatestTimestamp(String table) {
//        CassandraJavaRDD<CassandraRow> limit = scjf.cassandraTable(keyspace, table).select("last_parsed").limit(1l);
//        List<CassandraRow> collect = limit.collect();
//        return collect.get(0).getLong("last_parsed");
        // dummy.
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) - numDaysBatch);
        return now.getTimeInMillis();
    }

    /**
     * load articles in a tuple4 format
     * <br>
     * we need title for allocating it in clusters
     *
     * @param timestamp from epoch
     * @return <entry_url, title, clean_text, timestamp>
     */
    public JavaRDD<Tuple4<String, String, String, Long>> loadArticlesPublishedLaterThan(long timestamp) {
        // TODO improve query: try to filter by timestamp on cassandra, not afterwards
        //System.out.println("DEBUG - LIMIT up to 20 articles"); // fixme
        JavaRDD<CassandraRow> filter = scjf
                .cassandraTable(keyspace, Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
        .select(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(),
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(),
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName())
                // DEBUG // TODO: COMMENT OUT! - gets the first K
                                .limit(10l);
                // DEBUG // TODO: UNCOMMENT!
                //.filter(new FilterByAfterTimeStamp(timestamp));
        JavaRDD<Tuple4<String, String, String, Long>> extracted = filter.map(new CassandraArticleRowToTuple4RDD(
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(),
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(),
                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName())
        );
        return extracted;
    }
}
