/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.clustering.util;

import com.datastax.spark.connector.japi.CassandraRow;
import gr.demokritos.iit.base.repository.views.Cassandra;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class FilterByAfterTimeStamp implements Function<CassandraRow, Boolean> {

    private final long timest;

    public FilterByAfterTimeStamp(long timestamp) {
        this.timest = timestamp;
    }

    @Override
    public Boolean call(CassandraRow arg0) throws Exception {
        return arg0.getLong(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName()) >= timest;
    }
}
