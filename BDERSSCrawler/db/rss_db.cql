CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1}; // dev

-- CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 3}; // prod

id	int(11) AI PK
entry_url	varchar(767)
feed_url	varchar(767)
crawl_id	bigint(20)
raw_text	mediumtext
clean_text	mediumtext
published	bigint(20)
crawled	bigint(20)
language	varchar(5)

CREATE TABLE bde.news_entries_per_date (
    year_month_literal varchar,
    published timestamp,
    entry_url varchar,
    place varchar,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language timestamp,
    primary key ((year_month_literal), published, entry_url)
) WITH CLUSTERING ORDER BY (published DESC, entry_url ASC);

create index entry_place_idx on bde.news_entries_per_date(place);

CREATE TABLE bde.news_entries_per_place (
    place_literal varchar,
    place timestamp,
    entry_url varchar,
    place varchar,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language timestamp,
    primary key ((year_month_literal), published, entry_url)
) WITH CLUSTERING ORDER BY (published DESC, entry_url ASC);