CREATE DATABASE IF NOT EXISTS bde_twitter DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
USE bde_twitter;
CREATE USER 'bdeuser'@'localhost' IDENTIFIED BY 'resuedb16';
GRANT ALL PRIVILEGES ON bde_twitter.* TO 'bdeuser'@'localhost';
FLUSH PRIVILEGES;

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

