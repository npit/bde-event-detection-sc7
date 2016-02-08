CREATE DATABASE IF NOT EXISTS bde DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;
USE bde;

-- We assume that the java process is on the same machine as the database
CREATE USER 'bdeuser'@'localhost' IDENTIFIED BY 'bdepw' ;
GRANT ALL PRIVILEGES ON bde.* TO 'bdeuser'@'localhost' WITH GRANT OPTION;

-- The tables
-- Note that start must ALWAYS be set otherwise MySql will automatically update it to the current time. This does not however apply to end. MySql decides which rule to apply based on the order of the columns in the create statement.
CREATE TABLE IF NOT EXISTS blog_crawls (id BIGINT NOT NULL UNIQUE, PRIMARY KEY(id), start BIGINT NOT NULL, end BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS news_crawls (id BIGINT NOT NULL UNIQUE, PRIMARY KEY(id), start BIGINT NOT NULL, end BIGINT NOT NULL);

CREATE TABLE IF NOT EXISTS blog_feeds (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), feed_url VARCHAR(1000) NOT NULL, UNIQUE KEY(feed_url), etag VARCHAR(255) NULL, last_modified VARCHAR(255) NULL);
CREATE TABLE IF NOT EXISTS news_feeds  (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), feed_url VARCHAR(1000) NOT NULL, UNIQUE KEY(feed_url), etag VARCHAR(255) NULL, last_modified VARCHAR(255) NULL);

CREATE TABLE IF NOT EXISTS blog_articles (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), entry_url VARCHAR(1000) NOT NULL, UNIQUE KEY(entry_url), feed_url VARCHAR(1000) NOT NULL, crawl_id BIGINT NOT NULL, raw_text MEDIUMTEXT, clean_text MEDIUMTEXT, analysed_text MEDIUMTEXT, published BIGINT, crawled BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS news_articles (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), entry_url VARCHAR(1000) NOT NULL, UNIQUE KEY(entry_url), feed_url VARCHAR(1000) NOT NULL, crawl_id BIGINT NOT NULL, raw_text MEDIUMTEXT, clean_text MEDIUMTEXT, analysed_text MEDIUMTEXT, published BIGINT, crawled BIGINT NOT NULL);
