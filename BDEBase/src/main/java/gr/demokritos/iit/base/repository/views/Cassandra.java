/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.base.repository.views;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class Cassandra {

    public static class Twitter {

        // cassandra tables
        public enum Tables {

            TWITTER_SOURCE("twitter_source"),
            TWITTER_USER("twitter_user"),
            TWITTER_POST("twitter_post"),
            TWITTER_POSTS_PER_DATE("twitter_posts_per_date"),
            TWITTER_POSTS_PER_HASHTAG("twitter_posts_per_hashtag"),
            TWITTER_POSTS_PER_EXTERNAL_URL("twitter_posts_per_external_url"),
            TWITTER_POSTS_PER_COORDINATES("twitter_posts_per_coordinates"),
            TWITTER_POSTS_PER_PLACE("twitter_posts_per_place"),
            TWITTER_POSTS_PER_ENGINE("twitter_posts_per_engine"),
            TWITTER_LOG("twitter_log");
            private final String table_name;

            private Tables(String table_name) {
                this.table_name = table_name;
            }

            public String getTableName() {
                return table_name;
            }
        }

        // cassandra table fields
        public enum TBL_TWITTER_SOURCE {

            FLD_ACCOUNT_NAME("account_name"),
            FLD_ACTIVE("active");
            private final String column;

            private TBL_TWITTER_SOURCE(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_USER {

            FLD_USER_ID("user_id"),
            FLD_FOLLOWERS_CNT("followers_count"),
            FLD_FRIENDS_CNT("friends_count"),
            FLD_LISTED_CNT("listed_count"),
            FLD_NAME("name"),
            FLD_ACCOUNT_NAME("account_name"),
            FLD_LOCATION("location"),
            FLD_STATUSES_CNT("statuses_count"),
            FLD_TIMEZONE("timezone");
            private final String column;

            private TBL_TWITTER_USER(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POST {

            FLD_POST_ID("post_id"),
            FLD_CREATED_AT("created_at"),
            FLD_COORDINATES("coordinates"),
            FLD_PLACE("place"),
            FLD_RETWEET_CNT("retweet_count"),
            FLD_FOLLOWERS_WHEN_PUBLISHED("followers_when_published"),
            FLD_TWEET("tweet"),
            FLD_LANGUAGE("language"),
            FLD_PERMALINK("url"),
            FLD_EXTERNAL_LINKS("external_links"),
            FLD_ACCOUNT_NAME("account_name");
            private final String column;

            private TBL_TWITTER_POST(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POSTS_PER_HASHTAG {

            FLD_HASHTAG("hashtag"),
            FLD_CREATED_AT("created_at"),
            FLD_POST_ID("post_id"),
            FLD_ACCOUNT_NAME("account_name"),
            FLD_LANGUAGE("language"),
            FLD_TWEET("tweet"),
            FLD_PERMALINK("url");

            private final String column;

            private TBL_TWITTER_POSTS_PER_HASHTAG(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POSTS_PER_EXTERNAL_URL {

            FLD_EXTERNAL_URL("external_url"),
            FLD_CREATED_AT("created_at"),
            FLD_POST_ID("post_id"),
            FLD_ACCOUNT_NAME("account_name"),
            FLD_LANGUAGE("language"),
            FLD_TWEET("tweet"),
            FLD_PERMALINK("url");

            private final String column;

            private TBL_TWITTER_POSTS_PER_EXTERNAL_URL(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POSTS_PER_DATE {

            FLD_YEAR_MONTH_DAY_BUCKET("year_month_day_bucket"),
            FLD_CREATED_AT("created_at"),
            FLD_POST_ID("post_id"),
            FLD_ACCOUNT_NAME("account_name"),
            FLD_LANGUAGE("language"),
            FLD_TWEET("tweet"),
            FLD_PERMALINK("url");

            private final String column;

            private TBL_TWITTER_POSTS_PER_DATE(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POSTS_PER_COORDINATE {

            FLD_GEO_BUCKET("geo_bucket"),
            FLD_CREATED_AT("created_at"),
            FLD_GEOLOCATION("geolocation"),
            FLD_POST_ID("post_id"),
            FLD_ACCOUNT_NAME("account_name"),
            FLD_LANGUAGE("language"),
            FLD_TWEET("tweet"),
            FLD_PERMALINK("url");

            private final String column;

            private TBL_TWITTER_POSTS_PER_COORDINATE(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POSTS_PER_PLACE {

            FLD_PLACE_LITERAL("place_literal"),
            FLD_CREATED_AT("created_at"),
            FLD_PLACE("place"),
            FLD_POST_ID("post_id"),
            FLD_ACCOUNT_NAME("account_name"),
            FLD_LANGUAGE("language"),
            FLD_TWEET("tweet"),
            FLD_PERMALINK("url");

            private final String column;

            private TBL_TWITTER_POSTS_PER_PLACE(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_POSTS_PER_ENGINE {

            FLD_ENGINE_TYPE("engine_type"),
            FLD_ENGINE_ID("engine_id"),
            FLD_POST_ID("post_id");

            private final String column;

            private TBL_TWITTER_POSTS_PER_ENGINE(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

        public enum TBL_TWITTER_LOG {

            FLD_ENGINE_TYPE("engine_type"),
            FLD_ENGINE_ID("engine_id"),
            FLD_STARTED("started"),
            FLD_ENDED("ended");

            private final String column;

            private TBL_TWITTER_LOG(String columnn) {
                this.column = columnn;
            }

            public String getColumnName() {
                return column;
            }
        }

    }

    public static class RSS {

        public enum Tables {

            // we implicitly place news_articles tables names here, though news_crawler
            // uses a different way to call the table name, in order to be consistent with
            // the other modules. 

            NEWS_ARTICLES("news_articles"),
            NEWS_ARTICLES_PER_PUBLISHED_DATE("news_articles_per_published_date"),
            NEWS_ARTICLES_PER_CRAWLED_DATE("news_articles_per_crawled_date"),
            NEWS_ARTICLES_PER_PLACE("news_articles_per_place"),
            NEWS_FEEDS("news_feeds"),
            NEWS_CRAWLS("news_crawls");
            private final String table;

            private Tables(String table) {
                this.table = table;
            }

            public String getTableName() {
                return table;
            }

        }

        public enum TBL_ARTICLES {

            FLD_REVERSED_HOST("reversed_host"),
            FLD_ENTRY_URL("entry_url"),
            FLD_PUBLISHED("published"),
            FLD_PLACE_LITERAL("place_literal"),
            FLD_FEED_URL("feed_url"),
            FLD_CRAWL_ID("crawl_id"),
            FLD_RAW_TEXT("raw_text"),
            FLD_CLEAN_TEXT("clean_text"),
            FLD_CRAWLED("crawled"),
            FLD_LANGUAGE("language");

            private final String columnn;

            private TBL_ARTICLES(String columnn) {
                this.columnn = columnn;
            }

            public String getColumnName() {
                return columnn;
            }
        }

        public enum TBL_ARTICLES_PER_DATE {

            FLD_YEAR_MONTH_DAY_BUCKET("year_month_day_literal"),
            FLD_PUBLISHED("published"),
            FLD_ENTRY_URL("entry_url"),
            FLD_PLACE_LITERAL("place_literal"),
            FLD_FEED_URL("feed_url"),
            FLD_CRAWL_ID("crawl_id"),
            FLD_RAW_TEXT("raw_text"),
            FLD_CLEAN_TEXT("clean_text"),
            FLD_CRAWLED("crawled"),
            FLD_LANGUAGE("language");

            private final String columnn;

            private TBL_ARTICLES_PER_DATE(String columnn) {
                this.columnn = columnn;
            }

            public String getColumnName() {
                return columnn;
            }
        }

        public enum TBL_ARTICLES_PER_PLACE {

            FLD_PLACE_LITERAL("place_literal"),
            FLD_PUBLISHED("published"),
            FLD_ENTRY_URL("entry_url"),
            FLD_PLACE("tplace"),
            FLD_FEED_URL("feed_url"),
            FLD_CRAWL_ID("crawl_id"),
            FLD_RAW_TEXT("raw_text"),
            FLD_CLEAN_TEXT("clean_text"),
            FLD_CRAWLED("crawled"),
            FLD_LANGUAGE("language");

            private final String columnn;

            private TBL_ARTICLES_PER_PLACE(String columnn) {
                this.columnn = columnn;
            }

            public String getColumnName() {
                return columnn;
            }
        }

        public enum TBL_FEEDS {

            FLD_FEED_URL("feed_url"),
            FLD_ETAG("etag"),
            FLD_LAST_MODIFIED("last_modified");

            private final String columnn;

            private TBL_FEEDS(String columnn) {
                this.columnn = columnn;
            }

            public String getColumnName() {
                return columnn;
            }
        }

        public enum TBL_CRAWLS {

            FLD_CRAWL_TYPE("crawl_type"),
            FLD_CRAWL_ID("crawl_id"),
            FLD_START("start"),
            FLD_END("end");

            private final String columnn;

            private TBL_CRAWLS(String columnn) {
                this.columnn = columnn;
            }

            public String getColumnName() {
                return columnn;
            }
        }
    }

    public static class Location {

        public enum Table {

            LOCATION_LOG("location_extraction_log");
            private String table_name;

            private Table(String name) {
                this.table_name = name;
            }

            public String getTableName() {
                return table_name;
            }
        }

        public enum TBL_LOCATION_LOG {

            FLD_SCHEDULE_TYPE("schedule_type"),
            FLD_SCHEDULE_ID("schedule_id"),
            FLD_START("start"),
            FLD_END("end"),
            FLD_LAST_PARSED("last_parsed"),
            FLD_ITEMS_UPDATED("items_updated");
            private String column;

            private TBL_LOCATION_LOG(String column) {
                this.column = column;
            }

            public String getColumnName() {
                return column;
            }

        }

    }
}
