CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1}; // dev

-- CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 3}; // prod

CREATE TABLE IF NOT EXISTS bde.location_extraction_log (
    -- the same as from the crawler.
    schedule_type varchar,
    schedule_id bigint,
    start bigint,
    end bigint,
    items_updated bigint,
    last_parsed bigint,
    primary key ((schedule_type), schedule_id)
)WITH CLUSTERING ORDER BY (schedule_id DESC);