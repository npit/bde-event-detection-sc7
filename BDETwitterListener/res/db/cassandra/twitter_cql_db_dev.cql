DROP KEYSPACE bde_twitter;

CREATE KEYSPACE bde_twitter WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1};
-- CREATE KEYSPACE bde_twitter WITH replication = {'class' : 'NetworkTopologyStrategy', 'replication_factor' : 3}; // prod

CREATE TABLE bde_twitter.twitter_source (
  account_name text,
  active boolean,
  primary key (account_name)
);

create index source_active_idx on bde_twitter.twitter_source(active);

CREATE TABLE bde_twitter.twitter_user (
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

create index user_account_name_idx on bde_twitter.twitter_user(account_name);

CREATE TABLE bde_twitter.twitter_post (
  post_id bigint,
  created_at timestamp,
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

create index post_account_name_idx on bde_twitter.twitter_post(account_name);

CREATE TABLE bde_twitter.twitter_posts_per_date (
    year_month_bucket varchar,
    created_at timestamp,
    post_id bigint,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((year_month_bucket), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

create index language_created_at_idx on bde_twitter.twitter_posts_per_date(language);

CREATE TABLE bde_twitter.twitter_posts_per_hashtag (
    hashtag text,
    post_id bigint,
    created_at timestamp,
    tweet text,    
    language text,
    url text,
    account_name text,
    primary key ((hashtag), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TABLE bde_twitter.twitter_posts_per_external_url (
    external_url text,
    post_id bigint,
    created_at timestamp,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((external_url), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TYPE bde_twitter.twitter_geolocation (latitude double, longitude double);

CREATE TABLE bde_twitter.twitter_posts_per_coordinates (
    geo_bucket varchar,
    geolocation frozen<twitter_geolocation>,
    created_at timestamp,
    post_id bigint,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((geo_bucket), created_at, geolocation, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, geolocation DESC, post_id DESC);

CREATE TYPE bde_twitter.twitter_place (
    id varchar,
    full_name varchar,
    name varchar,
    street_address varchar,
    country_code varchar,
    country varchar,
    place_type varchar,
    url varchar,
    bounding_box_type varchar,
    geometry_type varchar
);

CREATE TABLE bde_twitter.twitter_posts_per_place (
    place_literal varchar,
    created_at timestamp,
    post_id bigint,
    place frozen<twitter_place>,
    tweet text,
    language text,
    url text,
    account_name text,
    primary key ((place_literal), created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC, post_id DESC);

CREATE TABLE bde_twitter.twitter_log (
  engine_type text,
  engine_id bigint,
  started timestamp, 
  ended timestamp,
  primary key ((engine_type), engine_id)
) WITH CLUSTERING ORDER BY (engine_id DESC);

CREATE TABLE bde_twitter.twitter_posts_per_engine (
  engine_type text,
  engine_id bigint,
  post_id bigint,
  primary key ((engine_type), engine_id, post_id)
) WITH CLUSTERING ORDER BY (engine_id DESC, post_id DESC);