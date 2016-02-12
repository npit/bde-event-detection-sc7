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
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.structs.LocSched;
import java.util.Date;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraLocationRepository extends BaseCassandraRepository implements ILocationRepository {

    private static final String SCHEDULE_TYPE = "location_extraction";

    public CassandraLocationRepository(Session session) {
        super(session);
    }

    @Override
    public LocSched scheduleInitialized() {
        Statement select = QueryBuilder
                .select(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName())
                .from(session.getLoggedKeyspace(), Cassandra.Location.Table.LOCATION_LOG.getTableName())
                .where(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), SCHEDULE_TYPE)).limit(1);
        ResultSet results = session.execute(select);

        long max_existing = 0l;
        long last_parsed = 0l;

        Row one = results.one();
        if (one != null) {
            max_existing = one.getLong(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName());
            last_parsed = one.getLong(Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName());
        }
        long current = max_existing + 1;
        LocSched curSched = new LocSched(current, last_parsed);

        Statement insert = QueryBuilder
                .insertInto(session.getLoggedKeyspace(), Cassandra.Location.Table.LOCATION_LOG.getTableName())
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), SCHEDULE_TYPE)
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName(), current)
                .value(Cassandra.Location.TBL_LOCATION_LOG.FLD_START.getColumnName(), new Date().getTime());
        session.execute(insert);
        return curSched;
    }

    @Override
    public void scheduleFinalized(LocSched sched) {
        Statement update = QueryBuilder
                .update(session.getLoggedKeyspace(), Cassandra.Location.Table.LOCATION_LOG.getTableName())
                .with(set(Cassandra.Location.TBL_LOCATION_LOG.FLD_END.getColumnName(), new Date().getTime()))
                .and(set(Cassandra.Location.TBL_LOCATION_LOG.FLD_LAST_PARSED.getColumnName(), sched.getLastParsed()))
                .and(set(Cassandra.Location.TBL_LOCATION_LOG.FLD_ITEMS_UPDATED.getColumnName(), sched.getItemsUpdated()))
                .where(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_TYPE.getColumnName(), SCHEDULE_TYPE))
                .and(eq(Cassandra.Location.TBL_LOCATION_LOG.FLD_SCHEDULE_ID.getColumnName(), sched.getScheduleID()));
        session.execute(update);
    }

    @Override
    public void updateArticleWithPlacesLiteral(String permalink, Set<String> places) {
        // TODO implement.
        // update articles per place. Insert All data.
        System.out.println(String.format("updating %s with places: %s", permalink, places.toString()));
    }

}
