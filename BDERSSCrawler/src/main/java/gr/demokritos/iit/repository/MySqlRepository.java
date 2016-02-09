/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.repository;

import com.google.common.collect.Lists;
import com.sun.syndication.feed.synd.SyndEntry;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.model.Content;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.crawlers.schedule.CrawlStrategy;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.model.UrlMetaData;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.sql.DataSource;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class MySqlRepository extends AbstractRepository implements IRepository {

    private final DataSource connectionPool;
    private final String databaseName;

    public static IRepository createBlogRepository(DataSource connectionPool, String databaseName) {
        return new MySqlRepository(connectionPool, databaseName, CrawlStrategy.BLOG, CrawlStrategy.BLOG.extractor());
    }

    public static IRepository createNewsRepository(DataSource connectionPool, String databaseName) {
        return new MySqlRepository(connectionPool, databaseName, CrawlStrategy.NEWS, CrawlStrategy.NEWS.extractor());
    }

    private MySqlRepository(DataSource connectionPool, String databaseName, CrawlStrategy crawlerStrategy,
            BoilerpipeExtractor extractor) {
        super(crawlerStrategy, extractor);
        this.connectionPool = connectionPool;
        this.databaseName = databaseName;
    }

    @Override
    public List<SyndEntry> identifyNewEntries(Item item, List<SyndEntry> candidateEntries) {
        return findNewEntries(item, candidateEntries);
    }

    @Override
    public boolean isNewEntry(String link) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionPool.getConnection();
            String query = "select entry_url from " + databaseName + "." + crawlerStrategy.crawlType() + "_articles"
                    + " where entry_url = ? limit 1;";
            statement = connection.prepareStatement(query);
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return true;
            } else {
                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, resultSet);
        }
    }

    @Override
    public void savePage(Item item, Content content, Date publishedDate) throws IOException, BoilerpipeProcessingException {
        String cleanText = extractor.getText(content.getRawText());

        // For backwards compatibility with the old versions of the crawler which didn't do any cleaning we
        // store null when we don't successfully clean some text.
        if (cleanText.isEmpty()) {
            cleanText = null;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionPool.getConnection();
            String query = "insert into " + databaseName + "." + crawlerStrategy.crawlType() + "_articles"
                    + "(entry_url, crawl_id, feed_url, raw_text, clean_text, published, crawled)"
                    + " values(?, ?, ?, ?, ?, ?, ?) on duplicate key update crawl_id = ?, raw_text = ?, clean_text = ?,"
                    + " published = ?, crawled = ?;";
            statement = connection.prepareStatement(query);
            long published = calculatePublishedValue(publishedDate);
            long crawled = content.getCrawlDate().getTime();

            statement.setString(1, content.getUrl());
            CrawlId crawlId = item.getCrawlId();
            statement.setLong(2, crawlId.getId());
            statement.setString(3, item.getFeedUrl());
            statement.setString(4, content.getRawText());
            statement.setString(5, cleanText);
            statement.setLong(6, published);
            statement.setLong(7, crawled);
            statement.setLong(8, crawlId.getId());
            statement.setString(9, content.getRawText());
            statement.setString(10, cleanText);
            statement.setLong(11, published);
            statement.setLong(12, crawled);

            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, null);
        }
    }

    @Override
    public CrawlId findMostRecentCrawlId() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionPool.getConnection();
            String query = "select id, start, end from " + databaseName + "." + crawlerStrategy.crawlType()
                    + "_crawls order by id desc limit 1;";
            statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                long crawlId = resultSet.getLong(1);
                long start = resultSet.getLong(2);
                long end = resultSet.getLong(3);
                return new CrawlId(crawlId, start, end);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, resultSet);
        }
        return null;
    }

    @Override
    public void saveCrawlId(CrawlId crawlId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionPool.getConnection();
            String query = "insert into " + databaseName + "." + crawlerStrategy.crawlType()
                    + "_crawls(id, start, end) values(?, ?, ?) on duplicate key "
                    + "update id = ?, start = ?, end = ?";
            statement = connection.prepareStatement(query);
            statement.setLong(1, crawlId.getId());
            statement.setLong(2, crawlId.getStartTimestamp());
            statement.setLong(3, crawlId.getEndTimestamp());
            statement.setLong(4, crawlId.getId());
            statement.setLong(5, crawlId.getStartTimestamp());
            statement.setLong(6, crawlId.getEndTimestamp());

            statement.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, null);
        }
    }

    @Override
    public UrlMetaData getFeedMetadata(String url) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionPool.getConnection();
            String query = "select etag, last_modified from " + databaseName + "." + crawlerStrategy.crawlType()
                    + "_feeds where feed_url = ?;";
            statement = connection.prepareStatement(query);
            statement.setString(1, url);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String etag = resultSet.getString(1);
                String lastModified = resultSet.getString(2);
                return new UrlMetaData(etag, lastModified);
            } else {
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, resultSet);
        }
    }

    @Override
    public void updateFeedMetaData(Content content) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionPool.getConnection();
            String query = "insert into " + databaseName + "." + crawlerStrategy.crawlType()
                    + "_feeds(feed_url, etag, last_modified) values(?, ?, ?) "
                    + "on duplicate key update etag = ?, last_modified = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, content.getUrl());
            statement.setString(2, content.getEtag());
            statement.setString(3, content.getLastModified());
            statement.setString(4, content.getEtag());
            statement.setString(5, content.getLastModified());
            statement.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, null);
        }
    }

    @Override
    public List<String> find(String entryUrl) {
        List<String> results = Lists.newArrayList();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionPool.getConnection();
            String query = "select entry_url, feed_url, crawl_id, raw_text, clean_text, published, crawled from " + databaseName + "."
                    + crawlerStrategy.crawlType() + "_articles where entry_url = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, entryUrl);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                results.add(resultSet.getString("crawl_id"));
                results.add(resultSet.getString("feed_url"));
                results.add(resultSet.getString("entry_url"));
                results.add(resultSet.getString("raw_text"));
                results.add(resultSet.getString("clean_text"));
                results.add(resultSet.getString("crawled"));
                results.add(resultSet.getString("published"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            release(connection, statement, resultSet);
        }

        return results;
    }

//    public void deleteAllTestData() {
//        deleteTablesFromTestDatabase(crawlerStrategy.crawlerType() + "_crawls");
//        deleteTablesFromTestDatabase(crawlerStrategy.crawlerType() + "_feeds");
//        deleteTablesFromTestDatabase(crawlerStrategy.crawlerType() + "_articles");
//    }
//
//    private void deleteTablesFromTestDatabase(String tableName) {
//        Connection connection = null;
//        PreparedStatement statement = null;
//        try {
//            connection = connectionPool.getConnection();
//            //Note that this is meant to be hardcoded to the test database
//            String query = "delete from bdetest." + tableName + ";";
//            statement = connection.prepareStatement(query);
//            statement.execute();
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            release(connection, statement, null);
//        }
//    }
    protected void release(Connection connection, Statement statement, ResultSet resultSet) {
        closeResultSet(resultSet);
        closeStatement(statement);
        closeConnection(connection);
    }

    private void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeStatement(Statement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        if (resultSet == null) {
            return;
        }
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
