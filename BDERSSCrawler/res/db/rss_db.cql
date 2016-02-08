CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1}; // dev

-- CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 3}; // prod

CREATE TABLE bde.news_entries (
    entry_url varchar,
    published timestamp,
    place_literal varchar,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language varchar,
    primary key ((entry_url), published)
) WITH CLUSTERING ORDER BY (published DESC);

CREATE TABLE bde.news_entries_per_published_date (
    year_month_day_literal varchar,
    published timestamp,
    entry_url varchar,
    place_literal varchar,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language varchar,
    primary key ((year_month_day_literal), published, entry_url)
) WITH CLUSTERING ORDER BY (published DESC, entry_url ASC);

create index entry_place_idx on bde.news_entries_per_published_date(place_literal);
create index rss_crawlid_idx on bde.news_entries_per_published_date(crawl_id);
create index rss_lang_idx on bde.news_entries_per_published_date(language);

CREATE TABLE bde.news_entries_per_place (
    place_literal varchar,
    published timestamp,
    entry_url varchar,
    place frozen<tplace>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language varchar,
    primary key ((place_literal), published, entry_url)
) WITH CLUSTERING ORDER BY (published DESC, entry_url ASC);

create index crawlid_idx on bde.news_entries_per_place(crawl_id);
create index lang_idx on bde.news_entries_per_place(language);

CREATE TABLE bde.news_feeds (
    feed_url varchar, 
    etag varchar, 
    last_modified varchar,
    primary key (feed_url)
);

CREATE TABLE bde.news_crawls (
    crawl_id bigint, 
    start timestamp, 
    end timestamp,
    primary key (crawl_id)
);