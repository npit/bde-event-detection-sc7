CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1}; -- dev

-- CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 2}; -- prod

CREATE TABLE IF NOT EXISTS bde.events (
    event_id varchar,
    title text,
    description text,
    event_date varchar,
    place_mappings Map<text, text>,
    tweet_post_ids set<bigint>,
    event_source_urls set<varchar>,
    primary key (event_id)
);

-- save events per location
CREATE TABLE IF NOT EXISTS bde.events_per_place_literal (
    place_literal varchar,
    event_id varchar,
    title text,
    description text,
    event_date varchar,
    place_polygon text,
    tweet_post_ids set<bigint>,
    event_source_urls set<varchar>,
    primary key ((place_literal), event_id)
) WITH CLUSTERING ORDER BY (event_id DESC);