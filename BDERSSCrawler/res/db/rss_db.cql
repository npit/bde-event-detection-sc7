CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1}; // dev

-- CREATE KEYSPACE IF NOT EXISTS bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 3}; // prod

-- reverse host is used for cassandra bucket utilization

CREATE TABLE bde.news_articles (
    reversed_host varchar,
    entry_url varchar,
    published bigint,
    place_literal set<varchar>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled bigint,
    language varchar,
    primary key ((reversed_host), entry_url)
);

create index plain_crawlid_idx on bde.news_articles(crawl_id);
create index plain_lang_idx on bde.news_articles(language);

CREATE TABLE bde.news_articles_per_published_date (
    year_month_day_literal varchar,
    entry_url varchar,
    published bigint,
    place_literal set<varchar>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled bigint,
    language varchar,
    primary key ((year_month_day_literal), published, entry_url)
) WITH CLUSTERING ORDER BY (published DESC, entry_url ASC);

create index rss_crawlid_idx on bde.news_articles_per_published_date(crawl_id);
create index rss_lang_idx on bde.news_articles_per_published_date(language);

CREATE TABLE bde.news_articles_per_crawled_date (
    year_month_day_literal varchar,
    crawled bigint,
    entry_url varchar,
    place_literal set<varchar>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    published bigint,
    language varchar,
    primary key ((year_month_day_literal), crawled, entry_url)
) WITH CLUSTERING ORDER BY (crawled DESC, entry_url ASC);

create index rssc_crawlid_idx on bde.news_articles_per_crawled_date(crawl_id);
create index rssc_lang_idx on bde.news_articles_per_crawled_date(language);

CREATE TABLE bde.news_articles_per_place (
    place_literal varchar,
    entry_url varchar,
    published bigint,
    place frozen<tplace>,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled bigint,
    language varchar,
    primary key ((place_literal), entry_url)
) WITH CLUSTERING ORDER BY (entry_url ASC);

create index crawlid_idx on bde.news_articles_per_place(crawl_id);
create index lang_idx on bde.news_articles_per_place(language);

CREATE TABLE bde.news_feeds (
    feed_url varchar, 
    etag varchar, 
    last_modified varchar,
    primary key (feed_url)
);

-- crawl_type is 'news' always :-D but it's a nice way of sorting with IDs.
CREATE TABLE bde.news_crawls (
    crawl_type varchar,
    crawl_id bigint,
    start bigint, 
    end bigint,
    primary key ((crawl_type), crawl_id)
)WITH CLUSTERING ORDER BY (crawl_id DESC);