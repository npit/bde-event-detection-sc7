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