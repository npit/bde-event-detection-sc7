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

import java.util.Collection;
import java.util.LinkedHashMap;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import java.util.Map;
import twitter4j.Status;
import twitter4j.User;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IRepository {

    /**
     * global date format to utilize.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * load accounts to monitor. Returns all (active/inactive)
     *
     * @return the accounts to monitor from the repository
     */
    Collection<SourceAccount> getAccounts();

    /**
     * saves a userID (account name) in the DB. Always save as active=true
     *
     * @param accountName the twitter account name to save
     */
    void saveAccount(String accountName);

    /**
     *
     * @param sourceAcc
     * @return true if the source account name exists in the DB, either
     * active/inactive
     *
     */
    public boolean existSource(String sourceAcc);

    /**
     * save a user account for further monitoring
     *
     * @param accountName
     * @param active
     */
    void saveAccount(String accountName, boolean active);

    /**
     *
     * @param user the {@link User} object to insert
     * @return the user ID retrieved from the twitter API
     */
    long insertUser(User user);

    /**
     * update user statistics such as followers_count, friends_count,
     * listed_count, location, statuses_count, timezone
     *
     * @param user
     */
    void updateUser(User user);

    /**
     *
     * @param userID the user ID to check against
     * @return true if the user exists
     */
    public boolean existsUser(long userID);

    /**
     *
     * @param account_name
     * @return the user statistics
     */
    public Map<String, Object> getUserInfo(String account_name);

    /**
     * insert a new twitter post in the DB
     *
     * @param post
     * @param api_user_id
     * @param source_account_name
     * @param followersWhenPublished
     * @param engine_type
     * @param engine_id
     */
    void insertPost(Status post, long api_user_id, String source_account_name, int followersWhenPublished, CrawlEngine engine_type, long engine_id);

    /**
     * update the tweet's retweet_count value
     *
     * @param post the status to update
     */
    void updatePost(Status post);

    /**
     *
     * @param postID the (twitter api) post ID to check for existance
     * @return true if exists in DB, false otherwise
     */
    public boolean existsPost(long postID);

    /**
     * register a new schedule has started
     *
     * @param engine_type
     * @return
     */
    long scheduleInitialized(CrawlEngine engine_type);

    /**
     * register a schedule finalized
     *
     * @param schedule_id
     * @param engine_type
     */
    void scheduleFinalized(long schedule_id, CrawlEngine engine_type);

    LinkedHashMap<Integer, String> getUsersSortedByMaxRetweets();

    /**
     * the operations that the system supports.
     */
    public enum CrawlEngine {

        MONITOR("monitor"), SEARCH("search"), MONITOR_FOREVER("monitor_forever"), STREAM("stream"), FETCH("fetch");
        private String type;

        private CrawlEngine(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }
}
