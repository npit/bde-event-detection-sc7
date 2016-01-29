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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import java.util.Collection;
import java.util.LinkedHashMap;
import gr.demokritos.iit.crawlers.twitter.repository.nosql.MongoIO;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import gr.demokritos.iit.crawlers.twitter.url.DefaultURLUnshortener;
import gr.demokritos.iit.crawlers.twitter.structures.TwitterUser;
import java.util.Map;
import twitter4j.Status;
import twitter4j.User;

/**
 * NOT YET SUPPORTED
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 * @deprecated NOT YET SUPPORTED
 */
public class MongoRepository extends AbstractRepository implements IRepository {

    private DB db;

    private final String TWITTER_USER = "TWITTER_USER";
    private final String TWITTER_SOURCE = "TWITTER_SOURCE";
    private final String TWITTER_POST = "TWITTER_POST";
    private final String TWITTER_EXTERNAL_LINK = "TWITTER_EXTERNAL_LINK"; // need?
    private final String TWITTER_HASHTAG = "TWITTER_HASHTAG";
    // need?
    private final String TWITTER_POST_HAS_HASHTAG = "TWITTER_POST_HAS_HASHTAG";

    /**
     * Main constructor
     *
     * @param m
     * @param unshortenerArg
     */
    public MongoRepository(MongoIO m, DefaultURLUnshortener unshortenerArg) {
        super(unshortenerArg);
        this.db = m.getActiveDatabase();
    }

    public MongoRepository(MongoIO m) {
        this.db = m.getActiveDatabase();
    }

    /**
     * Not USED
     *
     * @param db
     * @param unshortenerArg
     */
    public MongoRepository(DB db, DefaultURLUnshortener unshortenerArg) {
        super(unshortenerArg);
        this.db = db;
    }

    @Override
    public void saveAccount(String accountName) {
        DBCollection col = db.getCollection(TWITTER_SOURCE);
        DBObject account = new BasicDBObject();
        account.put(SourceAccount.SOURCE, accountName);
        account.put(SourceAccount.ACTIVE, true);
        col.insert(account);
    }

    @Override
    public Collection<SourceAccount> getAccounts() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long insertUser(User user) {
        // get collection
        DBCollection col = db.getCollection(TWITTER_USER);
        // get user
        TwitterUser twUser = new TwitterUser(user);
        DBObject object = (DBObject) JSON.parse(twUser.toJSON());

        WriteResult wr = col.insert(object);

        System.out.println(wr.toString());

        return -1l;
    }

    @Override
    public void updateUser(User user) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updatePost(Status post) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean existsPost(long postID) {
        return false;
    }

    @Override
    public boolean existsUser(long userID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LinkedHashMap<Integer, String> getUsersSortedByMaxRetweets() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean existSource(String sourceAcc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveAccount(String accountName, boolean active) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void insertPost(Status post, long api_user_id, String source_account_name, int followersWhenPublished, CrawlEngine engine_type, long engine_id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long scheduleInitialized(CrawlEngine engine_type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void scheduleFinalized(long schedule_id, CrawlEngine engine_type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Object> getUserInfo(String account_name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
