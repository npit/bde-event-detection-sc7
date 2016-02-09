CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1}; // dev

-- CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 3}; // prod

CREATE TABLE bde.news_articles (
    entry_url varchar,
    published timestamp,
    place_literal set<varchar>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language varchar,
    primary key (entry_url)
);

create index published_idx on bde.news_articles(published);
create index plain_crawlid_idx on bde.news_articles(crawl_id);
create index plain_lang_idx on bde.news_articles(language);

CREATE TABLE bde.news_articles_per_published_date (
    year_month_day_literal varchar,
    entry_url varchar,
    published timestamp,
    place_literal set<varchar>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language varchar,
    primary key ((year_month_day_literal), entry_url, published)
) WITH CLUSTERING ORDER BY (entry_url ASC, published DESC);

create index rss_published on bde.news_articles_per_published_date(published);
create index rss_crawlid_idx on bde.news_articles_per_published_date(crawl_id);
create index rss_lang_idx on bde.news_articles_per_published_date(language);

CREATE TABLE bde.news_articles_per_place (
    place_literal varchar,
    entry_url varchar,
    published timestamp,
    place frozen<tplace>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled timestamp,
    language varchar,
    primary key ((place_literal), entry_url)
) WITH CLUSTERING ORDER BY (entry_url ASC);

create index pub_idx on bde.news_articles_per_place(published);
create index crawlid_idx on bde.news_articles_per_place(crawl_id);
create index lang_idx on bde.news_articles_per_place(language);

CREATE TABLE bde.news_feeds (
    feed_url varchar, 
    etag varchar, 
    last_modified varchar,
    primary key (feed_url)
);

CREATE TABLE bde.news_crawls (
    crawl_type varchar,
    crawl_id bigint,
    start timestamp, 
    end timestamp,
    primary key ((crawl_type), crawl_id)
)WITH CLUSTERING ORDER BY (crawl_id DESC);