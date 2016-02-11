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
package gr.demokritos.iit.crawlers.twitter.repository;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import gr.demokritos.iit.crawlers.twitter.url.IURLUnshortener;
import gr.demokritos.iit.base.util.langdetect.CybozuLangDetect;
import gr.demokritos.iit.crawlers.twitter.utils.SQLUtils;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.User;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class MySQLRepository extends AbstractRepository implements IRepository {

    private final DataSource dataSource;

    public MySQLRepository(DataSource dataSource) {
        super();
        this.dataSource = dataSource;
    }

    public MySQLRepository(DataSource dataSource, IURLUnshortener unshortenerArg) {
        super(unshortenerArg);
        this.dataSource = dataSource;
    }

    @Override
    public void saveAccount(String accountName) {
        String SQL_INSERT = "INSERT IGNORE INTO twitter_source(account_name) VALUES(?);";
        Connection dbConnection = null;
        PreparedStatement stmt = null;
        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(SQL_INSERT);
            stmt.setString(1, accountName);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, stmt, null);
        }
    }

    @Override
    public Collection<SourceAccount> getAccounts() {
        Collection<SourceAccount> lRes = new HashSet<>();
        Connection dbConnection = null;
        PreparedStatement stmt = null;
        ResultSet rSet = null;
        String SQL_SELECT = "SELECT account_name, active FROM twitter_source;";
        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(SQL_SELECT);
            // get accounts to crawl from the database
            rSet = stmt.executeQuery();
            while (rSet.next()) {
                String name = rSet.getString(1);
                boolean active = rSet.getBoolean(2);
                lRes.add(new SourceAccount(name, active));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, stmt, rSet);
        }
        return lRes;
    }

    /**
     *
     * @param user the {@link User} object to insert
     * @return the user ID generated
     */
    @Override
    public long insertUser(User user) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet generatedKeysSet = null;
        try {
            dbConnection = dataSource.getConnection();
            prepStmt
                    = dbConnection.prepareStatement("INSERT INTO twitter_user "
                            + "(`user_id`, `followers_count`, `friends_count`, "
                            + "`listed_count`, `name`, `screen_name`, `location`, `statuses_count`, `timezone`) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setLong(1, user.getId());
            prepStmt.setInt(2, user.getFollowersCount());
            prepStmt.setInt(3, user.getFriendsCount());
            prepStmt.setInt(4, user.getListedCount());
            prepStmt.setString(5, user.getName());
            prepStmt.setString(6, user.getScreenName());
            prepStmt.setString(7, user.getLocation());
            prepStmt.setInt(8, user.getStatusesCount());
            prepStmt.setString(9, user.getTimeZone());
            prepStmt.executeUpdate();
            return user.getId();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            SQLUtils.release(dbConnection, prepStmt, generatedKeysSet);
        }
    }

    @Override
    public void updateUser(User user) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = dataSource.getConnection();
            prepStmt
                    = dbConnection.prepareStatement("UPDATE twitter_user SET followers_count = ?, "
                            + "friends_count=?, listed_count=?, location=?, statuses_count=?, "
                            + "timezone=? WHERE user_id = ?;");
            prepStmt.setInt(1, user.getFollowersCount());
            prepStmt.setInt(2, user.getListedCount());
            prepStmt.setInt(3, user.getListedCount());
            prepStmt.setString(4, user.getLocation());
            prepStmt.setInt(5, user.getStatusesCount());
            prepStmt.setString(6, user.getTimeZone());
            prepStmt.setLong(7, user.getId());
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, prepStmt, null);
        }
    }

    @Override
    public void insertPost(Status post, long api_user_id, String source_account_name, int followersWhenPublished, CrawlEngine engine_type, long engine_id) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet generatedKeysSet = null;
        try {
            String sTweet = post.getText();
            // if nothing there (not really likely)
            if (sTweet == null || sTweet.trim().isEmpty()) {
                return;
            }
            String tweet_identified_lang = post.getLang();
            if (tweet_identified_lang == null || tweet_identified_lang.equalsIgnoreCase(TWEET_UNDEFINED_LANG)) {
                tweet_identified_lang = CybozuLangDetect.getInstance().identifyLanguage(cleanTweetFromURLs(post), TWEET_UNDEFINED_LANG);
            }
            Long postID = post.getId();
            dbConnection = dataSource.getConnection();
            prepStmt = dbConnection.prepareStatement(
                    "INSERT INTO twitter_post (post_id, created_at, coordinates, place, "
                    + "retweet_count, followers_when_published, text, language, url, "
                    + "twitter_user_id, engine_type, engine_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setLong(1, post.getId());
            Date createdAt = post.getCreatedAt();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = sdf.format(createdAt);
            prepStmt.setString(2, dateString);
            GeoLocation geoLocation = post.getGeoLocation();
            if (geoLocation == null) {
                prepStmt.setNull(3, Types.VARCHAR);
            } else {
                prepStmt.setString(3, post.getGeoLocation().toString());
            }
            Place place = post.getPlace();
            if (place != null) {
                String sFullName = place.getFullName();
                String sCountry = place.getCountry();
                if (sFullName != null) {
                    prepStmt.setString(4, sFullName);
                } else if (sCountry != null) {
                    prepStmt.setString(4, sCountry);
                }
            } else {
                prepStmt.setNull(4, Types.VARCHAR);
            }
            prepStmt.setLong(5, post.getRetweetCount());
            prepStmt.setInt(6, followersWhenPublished);
            prepStmt.setString(7, sTweet);
            prepStmt.setString(8, tweet_identified_lang);
            String url = "https://twitter.com/" + source_account_name + "/status/" + postID;
            prepStmt.setString(9, url);
            prepStmt.setLong(10, api_user_id);
            prepStmt.setString(11, engine_type.toString());
            prepStmt.setLong(12, engine_id);
            prepStmt.execute();
            // insert hashtags in database
            for (HashtagEntity htent : post.getHashtagEntities()) {
                insertHashtag(post.getId(), htent.getText());
            }
            // get URL links, if there 
            List<String> lsURLs = getURLEntities(post);
//            List<String> lsURLs = extractor.extractURLs(tweet);
            // unshorten URLs
            lsURLs = unshortenURLs(lsURLs);
            // insert them in the DB
            insertExternalURLs(post.getId(), lsURLs);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, prepStmt, generatedKeysSet);
        }
    }

    @Override
    public void updatePost(Status post) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = dataSource.getConnection();
            prepStmt
                    = dbConnection
                    .prepareStatement("UPDATE twitter_post SET retweet_count = ? WHERE post_id = ?;");
            prepStmt.setLong(1, post.getRetweetCount());
            prepStmt.setLong(2, post.getId());
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, prepStmt, null);
        }
    }

    /**
     * insert a hashtag related to a post
     *
     * @param post_id the post ID
     * @param hashtag the hashtag contained in the tweet
     */
    private void insertHashtag(long post_id, String hashtag) {
        Connection dbConnection = null;
        PreparedStatement insertHashtag = null;
        PreparedStatement insertPostToHashtag = null;
        try {
            dbConnection = dataSource.getConnection();
            insertHashtag
                    = dbConnection.prepareStatement("INSERT IGNORE INTO twitter_hashtag (hashtag) VALUES (?);");
            insertHashtag.setString(1, hashtag);
            insertHashtag.execute();
            insertHashtag.close();
            insertPostToHashtag
                    = dbConnection.prepareStatement("INSERT IGNORE INTO twitter_post_has_hashtag "
                            + "(`twitter_post_id`, `twitter_hashtag_id`) "
                            + "SELECT twitter_post.id, twitter_hashtag.id "
                            + "FROM twitter_post, twitter_hashtag "
                            + "WHERE twitter_post.id = ? AND twitter_hashtag.hashtag = ?;");
            insertPostToHashtag.setLong(1, post_id);
            insertPostToHashtag.setString(2, hashtag);
            insertPostToHashtag.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, insertPostToHashtag, null);
        }
    }

    /**
     * insert external URLs that were provided as links in a tweet.
     *
     * @param generatedID
     * @param lsURLs
     */
    private void insertExternalURLs(long post_id, List<String> lsURLs) {
        if (lsURLs == null || lsURLs.isEmpty()) {
            return; // nothing to add
        }
        Connection dbConnection = null;
        PreparedStatement insStmt = null;
        try {
            dbConnection = dataSource.getConnection();
            insStmt
                    = dbConnection.prepareStatement(
                            // url, post_id are UNIQUE pair in DB
                            "INSERT IGNORE INTO twitter_external_link (url, post_id) VALUES (?,?);"
                    );
            for (String extURL : lsURLs) {
                insStmt.setString(1, extURL);
                insStmt.setLong(2, post_id);
                // add batch to statement
                insStmt.addBatch();
            }
            insStmt.executeBatch();
        } catch (BatchUpdateException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, insStmt, null);
        }
    }

    /**
     *
     * @return total retweet count, name for each user in the DB
     */
    @Override
    public LinkedHashMap<Integer, String> getUsersSortedByMaxRetweets() {

        LinkedHashMap<Integer, String> lRes = new LinkedHashMap<>();

        String SQL = "select twitter_user.name, sum(twitter_post.retweet_count) as total_retweets from twitter_user "
                + "inner join "
                + "twitter_post on twitter_post.twitter_user_id = twitter_user.id "
                + "group by twitter_user.name "
                + "order by total_retweets desc;";
        Connection dbConnection = null;
        PreparedStatement selStmt = null;
        ResultSet rSet = null;
        try {
            dbConnection = dataSource.getConnection();
            selStmt
                    = dbConnection.prepareStatement(SQL);
            rSet = selStmt.executeQuery();
            while (rSet.next()) {
                Integer total = rSet.getInt(2);
                String name = rSet.getString(1);
                lRes.put(total, name);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, selStmt, rSet);
        }
        return lRes;
    }

    @Override
    public boolean existsPost(long postID) {
        boolean exists = false;
        String SELECT = "SELECT post_id FROM twitter_post WHERE post_id = ?;";
        Connection dbConnection = null;
        PreparedStatement pStmt = null;
        ResultSet resultSet = null;
        try {
            dbConnection = dataSource.getConnection();
            pStmt = dbConnection.prepareStatement(SELECT);
            pStmt.setLong(1, postID);
            resultSet = pStmt.executeQuery();
            if (resultSet.next()) {
                exists = true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, pStmt, resultSet);
        }
        return exists;
    }

    @Override
    public boolean existsUser(long userID) {
        boolean exists = false;
        String SELECT = "SELECT count(user_id) FROM twitter_user WHERE user_id = ?;";
        Connection dbConnection = null;
        PreparedStatement pStmt = null;
        ResultSet resultSet = null;
        try {
            dbConnection = dataSource.getConnection();
            pStmt = dbConnection.prepareStatement(SELECT);
            pStmt.setLong(1, userID);
            resultSet = pStmt.executeQuery();
            if (resultSet.next()) {
                exists = resultSet.getLong(1) > 0;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, pStmt, resultSet);
        }
        return exists;
    }

    @Override
    public long scheduleInitialized(CrawlEngine engine_type) {
        long latest_schedule_id = getLatestScheduleID(engine_type);
        return latest_schedule_id + 1;
    }

    @Override
    public void scheduleFinalized(long schedule_id, CrawlEngine engine_type) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        try {
            dbConnection = dataSource.getConnection();
            prepStmt
                    = dbConnection.prepareStatement("UPDATE twitter_log SET ended = ? "
                            + "WHERE engine_id = ? AND engine_type = ?;");
            prepStmt.setString(1, sdf.format(new Date()));
            prepStmt.setLong(2, schedule_id);
            prepStmt.setString(3, engine_type.toString().toLowerCase());
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, prepStmt, null);
        }
    }

    @Override
    public boolean existSource(String sourceAcc) {
        boolean exists = false;
        Connection dbConnection = null;
        PreparedStatement stmt = null;
        ResultSet rSet = null;
        String SQL_SELECT = "SELECT active FROM twitter_source WHERE account_name = ? LIMIT 1;";
        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(SQL_SELECT);
            stmt.setString(1, sourceAcc);
            rSet = stmt.executeQuery();
            if (rSet.next()) {
                exists = true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, stmt, rSet);
        }
        return exists;
    }

    @Override
    public void saveAccount(String accountName, boolean active) {
        String SQL_INSERT = "INSERT IGNORE INTO twitter_source(account_name, active) VALUES(?, ?);";
        Connection dbConnection = null;
        PreparedStatement stmt = null;
        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(SQL_INSERT);
            stmt.setString(1, accountName);
            stmt.setInt(2, active ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, stmt, null);
        }
    }

    private long getLatestScheduleID(CrawlEngine engine_type) {
        long latest = 0l;
        Connection dbConnection = null;
        PreparedStatement stmt = null;
        ResultSet rSet = null;
        String SQL_SELECT = "SELECT engine_id FROM twitter_log WHERE engine_type = ? ORDER BY engine_id DESC LIMIT 1;";
        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(SQL_SELECT);
            stmt.setString(1, engine_type.toString().toLowerCase());
            rSet = stmt.executeQuery();
            if (rSet.next()) {
                latest = rSet.getLong(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, stmt, rSet);
        }
        return latest;
    }

    @Override
    public Map<String, Object> getUserInfo(String account_name) {
        Map<String, Object> res = new HashMap();

        String SELECT = "SELECT `user_id`, `followers_count`, `friends_count`, "
                + "`listed_count`, `name`, `location`, `statuses_count`, `timezone`  FROM twitter_user WHERE screen_name = ?;";
        Connection dbConnection = null;
        PreparedStatement pStmt = null;
        ResultSet resultSet = null;
        try {
            dbConnection = dataSource.getConnection();
            pStmt = dbConnection.prepareStatement(SELECT);
            pStmt.setString(1, account_name);
            resultSet = pStmt.executeQuery();
            if (resultSet.next()) {
                long user_id = resultSet.getLong(1);
                res.put("user_id", user_id);
                long followers_count = resultSet.getLong(2);
                res.put("followers_count", followers_count);
                long friends_count = resultSet.getLong(3);
                res.put("friends_count", friends_count);
                long listed_count = resultSet.getLong(4);
                res.put("listed_count", listed_count);
                String name = resultSet.getString(5);
                res.put("name", name);
                String location = resultSet.getString(6);
                res.put("location", location);
                long statuses_count = resultSet.getLong(7);
                res.put("statuses_count", statuses_count);
                String timezone = resultSet.getString(8);
                res.put("timezone", timezone);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            SQLUtils.release(dbConnection, pStmt, resultSet);
        }
        return Collections.unmodifiableMap(res);
    }
}
