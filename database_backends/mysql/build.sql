CREATE DATABASE IF NOT EXISTS bde DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
USE bde;

-- We assume that the java process is on the same machine as the database
CREATE USER 'bdeuser'@'localhost' IDENTIFIED BY 'bdepw' ;
GRANT ALL PRIVILEGES ON bde.* TO 'bdeuser'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;

-- NEWS TABLES
-- The tables
-- Note that start must ALWAYS be set otherwise MySql will automatically update it to the current time. This does not however apply to end. MySql decides which rule to apply based on the order of the columns in the create statement.
CREATE TABLE IF NOT EXISTS blog_crawls (id BIGINT NOT NULL UNIQUE, PRIMARY KEY(id), start BIGINT NOT NULL, end BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS news_crawls (id BIGINT NOT NULL UNIQUE, PRIMARY KEY(id), start BIGINT NOT NULL, end BIGINT NOT NULL);

CREATE TABLE IF NOT EXISTS blog_feeds (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), feed_url VARCHAR(1000) NOT NULL, UNIQUE KEY(feed_url), etag VARCHAR(255) NULL, last_modified VARCHAR(255) NULL);
CREATE TABLE IF NOT EXISTS news_feeds  (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), feed_url VARCHAR(1000) NOT NULL, UNIQUE KEY(feed_url), etag VARCHAR(255) NULL, last_modified VARCHAR(255) NULL);

CREATE TABLE IF NOT EXISTS blog_articles (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), entry_url VARCHAR(1000) NOT NULL, UNIQUE KEY(entry_url), feed_url VARCHAR(1000) NOT NULL, crawl_id BIGINT NOT NULL, raw_text MEDIUMTEXT, clean_text MEDIUMTEXT, analysed_text MEDIUMTEXT, published BIGINT, crawled BIGINT NOT NULL, language VARCHAR(2), title text);
CREATE TABLE IF NOT EXISTS news_articles (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), entry_url VARCHAR(1000) NOT NULL, UNIQUE KEY(entry_url), feed_url VARCHAR(1000) NOT NULL, crawl_id BIGINT NOT NULL, raw_text MEDIUMTEXT, clean_text MEDIUMTEXT, analysed_text MEDIUMTEXT, published BIGINT, crawled BIGINT NOT NULL, language VARCHAR(2), title text);

-- TWITTER TABLES


CREATE TABLE `twitter_source` (
  `account_name` varchar(45) NOT NULL,
  `active` tinyint(2) NOT NULL DEFAULT '1',
  PRIMARY KEY (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `twitter_user` (
  `user_id` bigint(20) NOT NULL,
  `followers_count` int(11) DEFAULT NULL,
  `friends_count` int(11) DEFAULT NULL,
  `listed_count` int(11) DEFAULT NULL,
  `name` varchar(45) DEFAULT NULL,
  `screen_name` varchar(45) DEFAULT NULL,
  `location` varchar(45) DEFAULT NULL,
  `statuses_count` int(11) DEFAULT NULL,
  `timezone` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `twitter_post` (
  `post_id` bigint(20) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `coordinates` varchar(45) DEFAULT NULL,
  `place` text,
  `retweet_count` bigint(20) DEFAULT NULL,
  `followers_when_published` int(11) DEFAULT NULL,
  `text` text,
  `language` varchar(2) DEFAULT NULL,
  `url` text,
  `twitter_user_id` bigint(20) NOT NULL,
  `engine_type` varchar(45) NOT NULL,
  `engine_id` bigint(20) NOT NULL,
  PRIMARY KEY (`post_id`),
  KEY `fk_twitter_post_twitter_user1_idx` (`twitter_user_id`),
  KEY `engine_type_idx` (`engine_type`(45)),
  CONSTRAINT `fk_twitter_post_twitter_user1` FOREIGN KEY (`twitter_user_id`) REFERENCES `twitter_user` (`user_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `twitter_hashtag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hashtag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `hashtag_UNIQUE` (`hashtag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `twitter_post_has_hashtag` (
  `twitter_post_id` bigint(20) NOT NULL,
  `twitter_hashtag_id` bigint(20) NOT NULL,
  PRIMARY KEY (`twitter_post_id`, `twitter_hashtag_id`),
  KEY `fk_twitter_post_has_twitter_hashtag_twitter_hashtag1_idx` (`twitter_hashtag_id`),
  KEY `fk_twitter_post_has_twitter_hashtag_twitter_post1_idx` (`twitter_post_id`),
  CONSTRAINT `fk_twitter_post_has_twitter_hashtag_twitter_hashtag1` FOREIGN KEY (`twitter_hashtag_id`) REFERENCES `twitter_hashtag` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_twitter_post_has_twitter_hashtag_twitter_post1` FOREIGN KEY (`twitter_post_id`) REFERENCES `twitter_post` (`post_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `twitter_external_link` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `url` varchar(255) DEFAULT NULL,
  `post_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index3` (`url`,`post_id`),
  KEY `fk_twitter_external_link_1` (`post_id`),
  CONSTRAINT `fk_twitter_external_link_1` FOREIGN KEY (`post_id`) REFERENCES `twitter_post` (`post_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `twitter_log` (
  `engine_type` varchar(45) NOT NULL,
  `engine_id` bigint(20) NOT NULL,
  `started` datetime DEFAULT NULL, 
  `ended` datetime DEFAULT NULL,
  PRIMARY KEY (`engine_type`, `engine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE 
    ALGORITHM = UNDEFINED 
    DEFINER = `infoasset`@`localhost` 
    SQL SECURITY DEFINER
VIEW `active_sources` AS
    select 
        `twitter_source`.`account_name` AS `account_name`
    from
        `twitter_source`
    where
        (`twitter_source`.`active` = 1);

