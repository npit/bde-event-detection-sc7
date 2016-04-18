DROP KEYSPACE IF EXISTS bde;

CREATE KEYSPACE bde WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1};
-- CREATE KEYSPACE bde WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 2}; // prod

CREATE TABLE bde.twitter_source (
  account_name text,
  active boolean,
  primary key (account_name)
);

create index source_active_idx on bde.twitter_source(active);

CREATE TABLE bde.twitter_user (
  user_id bigint,
  followers_count bigint,
  friends_count bigint,
  listed_count bigint,
  name text,
  account_name text,
  location text,
  statuses_count bigint,
  timezone text,
  primary key ((user_id))
);

create index user_account_name_idx on bde.twitter_user(account_name);

CREATE TABLE bde.twitter_post (
  post_id bigint,
  created_at bigint,
  coordinates text,
  place text,
  retweet_count bigint,
  followers_when_published bigint,
  tweet text,
  language text,
  url text,
  external_links set<text>,
  account_name text,
  primary key ((post_id), language) 
);

create index post_account_name_idx on bde.twitter_post(account_name);

CREATE TABLE bde.twitter_posts_per_date (
    year_month_day_bucket varchar,
    created_at bigint,
    post_id bigint,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((year_month_day_bucket), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

create index language_created_at_idx on bde.twitter_posts_per_date(language);

CREATE TABLE bde.twitter_posts_per_hashtag (
    hashtag text,
    post_id bigint,
    created_at bigint,
    tweet text,    
    language text,
    url text,
    account_name text,
    primary key ((hashtag), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TABLE bde.twitter_posts_per_external_url (
    external_url text,
    post_id bigint,
    created_at bigint,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((external_url), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TYPE bde.geolocation (latitude double, longitude double);

CREATE TABLE bde.twitter_posts_per_coordinates (
    geo_bucket varchar,
    geolocation frozen<geolocation>,
    created_at bigint,
    post_id bigint,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((geo_bucket), created_at, geolocation, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, geolocation DESC, post_id DESC);

CREATE TYPE bde.tplace (
    id varchar,
    full_name varchar,
    name varchar,
    street_address varchar,
    country_code varchar,
    country varchar,
    place_type varchar,
    url varchar,
    bounding_box_type varchar,
    geometry_type varchar,
    bounding_box varchar,
);

-- this place is the place of the registered user
CREATE TABLE bde.twitter_posts_per_place (
    place_literal varchar,
    created_at bigint,
    post_id bigint,
    place frozen<tplace>,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((place_literal), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

-- this place is the referred place of the tweet, if any referral is found
-- by the location extraction module
CREATE TABLE bde.twitter_posts_per_referred_place (
    place_literal varchar, -- the place name, i.e. Athens
    created_at bigint,
    post_id bigint,
    location varchar, -- the coordinates, in string format, i.e. {"type":"Polygon","coordinates":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]} // lng,lat []
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((place_literal), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TABLE bde.twitter_log (
  engine_type text,
  engine_id bigint,
  started bigint, 
  ended bigint,
  primary key ((engine_type), engine_id)
) WITH CLUSTERING ORDER BY (engine_id DESC);

CREATE TABLE bde.twitter_posts_per_engine (
  engine_type text,
  engine_id bigint,
  post_id bigint,
  primary key ((engine_type), engine_id, post_id)
) WITH CLUSTERING ORDER BY (engine_id DESC, post_id DESC);


-- news articles DB 

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
    title varchar,
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
    title varchar,
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
    title varchar,
    primary key ((year_month_day_literal), crawled, entry_url)
) WITH CLUSTERING ORDER BY (crawled DESC, entry_url ASC);

create index rssc_crawlid_idx on bde.news_articles_per_crawled_date(crawl_id);
create index rssc_lang_idx on bde.news_articles_per_crawled_date(language);

CREATE TABLE bde.news_articles_per_place (
    place_literal varchar,
    entry_url varchar,
    published bigint,
    bounding_box varchar,
    feed_url varchar,
    crawl_id bigint,
    raw_text text,
    clean_text text,
    crawled bigint,
    language varchar,
    title varchar,
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

-- location extraction tables

CREATE TABLE IF NOT EXISTS bde.location_extraction_log (
    schedule_type varchar,
    schedule_id bigint,
    start bigint,
    end bigint,
    items_updated bigint,
    last_parsed bigint,
    primary key ((schedule_type), schedule_id)
)WITH CLUSTERING ORDER BY (schedule_id DESC);

-- event detection tables

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