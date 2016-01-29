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
package gr.demokritos.iit.crawlers.twitter.stream.user;

import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import twitter4j.Status;
import twitter4j.TwitterStream;
import twitter4j.User;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public abstract class AbstractUserListener {

    protected final TwitterStream twitterStream;
    protected final IRepository repository;

    protected long engine_id;

    public AbstractUserListener(TwitterStream twitterStream, IRepository repos) {
        this.twitterStream = twitterStream;
        this.repository = repos;
        this.engine_id = repository.scheduleInitialized(IRepository.CrawlEngine.STREAM);
    }

    protected void processStatus(Status status) {
        // if it is a retweet, get the original tweet
        while (status.isRetweet()) {
            status = status.getRetweetedStatus();
        }
        // proceed with storing in twitter repository
        long postID = status.getId();
        User user = status.getUser();
        // FIXME: check for possible issues when calling API for geolocation, 
        // on possible locks
        synchronized (AbstractUserListener.class) {
            // check for existance of post in DB
            boolean exists = repository.existsPost(postID);
            // if post already in the db then update post and user info
            if (exists) {
                repository.updatePost(status);
                repository.updateUser(user);
            } else {
                // get User ID
                long userID = user.getId();
                // check if user exists in the DB
                boolean exists_user = repository.existsUser(userID);
                String sourceAcc = user.getScreenName();
                if (exists_user) {
                    // if user is in the database, update
                    repository.updateUser(user);
                } else {
                    // else insert
                    repository.insertUser(user);
                }
                // get source ID
                if (!repository.existSource(sourceAcc)) {
                    // also insert as a source
                    repository.saveAccount(sourceAcc, false);
                }
                // get followers of user when post was published
                int followersWhenPublished = user.getFollowersCount();
                // finally, insert the post in the DB
                repository.insertPost(status, userID, user.getScreenName(), followersWhenPublished, IRepository.CrawlEngine.STREAM, engine_id);
            }
        }
    }
}
