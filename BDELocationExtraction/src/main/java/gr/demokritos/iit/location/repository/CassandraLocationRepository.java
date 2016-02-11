/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.structs.LocSched;
import java.util.Date;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraLocationRepository extends BaseCassandraRepository implements ILocationRepository {

    private static final String SCHEDULE_TYPE = "location_extraction";
    private long items_updated;

    public CassandraLocationRepository(Session session) {
        super(session);
    }

    @Override
    public LocSched scheduleInitialized() {
        String key = TBL_LOCATION_LOG.FLD_SCHEDULE_ID.column;
        Statement select = QueryBuilder
                .select(key)
                .from(session.getLoggedKeyspace(), Table.LOCATION_LOG.table_name)
                .where(eq(TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.column, SCHEDULE_TYPE)).limit(1);
        ResultSet results = session.execute(select);
        Row one = results.one();

        long max_existing = 0l;
        long last_parsed = 0l;

        if (one != null) {
            max_existing = one.getLong(key);
            last_parsed = one.getLong(TBL_LOCATION_LOG.FLD_LAST_PARSED.column);
        }
        long current = max_existing + 1;
        LocSched curSched = new LocSched(current, last_parsed);

        Statement insert = QueryBuilder.insertInto(session.getLoggedKeyspace(), Table.LOCATION_LOG.table_name)
                .value(TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.column, SCHEDULE_TYPE)
                .value(TBL_LOCATION_LOG.FLD_SCHEDULE_ID.column, current)
                .value(TBL_LOCATION_LOG.FLD_START.column, new Date().getTime());
        session.execute(insert);
        return curSched;
    }

    @Override
    public void scheduleFinalized(LocSched sched) {
        Statement update = QueryBuilder
                .update(session.getLoggedKeyspace(), Table.LOCATION_LOG.table_name)
                .with(set(TBL_LOCATION_LOG.FLD_END.column, new Date().getTime()))
                .and(set(TBL_LOCATION_LOG.FLD_LAST_PARSED.column, sched.getLast_parsed()))
                .where(eq(TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.column, SCHEDULE_TYPE))
                .and(eq(TBL_LOCATION_LOG.FLD_SCHEDULE_ID.column, sched.getSchedule_id()));
        session.execute(update);
    }
    // TODO implement.

    @Override
    public void updateArticleWithPlacesLiteral(String permalink, Set<String> places) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    enum Table {

        LOCATION_LOG("location_extraction_log");
        private String table_name;

        private Table(String name) {
            this.table_name = name;
        }
    }

    enum TBL_LOCATION_LOG {

        FLD_SCHEDULE_TYPE("schedule_type"),
        FLD_SCHEDULE_ID("schedule_id"),
        FLD_START("start"),
        FLD_END("end"),
        FLD_LAST_PARSED("last_parsed"),
        FLD_ITEMS_UPDATED("items_updated");
        private String column;

        private TBL_LOCATION_LOG(String column) {
            this.column = column;
        }

    }

}
