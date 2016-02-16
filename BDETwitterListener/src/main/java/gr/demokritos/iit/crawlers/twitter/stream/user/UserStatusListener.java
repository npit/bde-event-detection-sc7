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

import static gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory.LOGGER;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import gr.demokritos.iit.crawlers.twitter.stream.IStreamConsumer;
import java.util.concurrent.atomic.AtomicInteger;
import twitter4j.DirectMessage;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterStream;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;

/**
 * use getStream() to follow your stream. Basic implementation of onStatus, to
 * persist data in the DB, and on some limitation events
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class UserStatusListener extends AbstractUserListener implements UserStreamListener, IStreamConsumer {

    private final AtomicInteger cnt;

    public UserStatusListener(TwitterStream twitterStream, IRepository repos) {
        super(twitterStream, repos);
        this.cnt = new AtomicInteger();
    }

    @Override
    public void getStream() {
        this.twitterStream.addListener(this);
        twitterStream.user();
    }

    @Override
    public void onStatus(Status status) {
        LOGGER.info(String.format("{cnt: %d, user: %s, post_id: %d}: %s", cnt.incrementAndGet(), status.getUser().getScreenName(), status.getId(), status.getText()));
        processStatus(status, engine_id);
    }

    @Override
    public void onStallWarning(StallWarning sw) {
        LOGGER.warning(String.format("received onStallWarning: {code: %s, message: %s, percentage: %d}", sw.getCode(), sw.getMessage(), sw.getPercentFull()));
    }

    @Override
    public void onTrackLimitationNotice(int i) {
        LOGGER.warning(String.format("received OnTrackLimitationNotice: %d", i));
    }

    @Override
    public void onException(Exception excptn) {
        LOGGER.severe(excptn.getMessage());
//        excptn.printStackTrace();
    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice sdn) {

    }

    @Override
    public void onScrubGeo(long l, long l1) {

    }

    @Override
    public void onDeletionNotice(long arg0, long arg1) {

    }

    @Override
    public void onFriendList(long[] arg0) {

    }

    @Override
    public void onFavorite(User arg0, User arg1, Status arg2) {

    }

    @Override
    public void onUnfavorite(User arg0, User arg1, Status arg2) {

    }

    @Override
    public void onFollow(User arg0, User arg1) {

    }

    @Override
    public void onUnfollow(User arg0, User arg1) {

    }

    @Override
    public void onDirectMessage(DirectMessage arg0) {

    }

    @Override
    public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {

    }

    @Override
    public void onUserListMemberDeletion(User arg0, User arg1, UserList arg2) {

    }

    @Override
    public void onUserListSubscription(User arg0, User arg1, UserList arg2) {

    }

    @Override
    public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {

    }

    @Override
    public void onUserListCreation(User arg0, UserList arg1) {

    }

    @Override
    public void onUserListUpdate(User arg0, UserList arg1) {

    }

    @Override
    public void onUserListDeletion(User arg0, UserList arg1) {

    }

    @Override
    public void onUserProfileUpdate(User arg0) {

    }

    @Override
    public void onUserSuspension(long arg0) {

    }

    @Override
    public void onUserDeletion(long arg0) {

    }

    @Override
    public void onBlock(User arg0, User arg1) {

    }

    @Override
    public void onUnblock(User arg0, User arg1) {

    }

    @Override
    public void onRetweetedRetweet(User arg0, User arg1, Status arg2) {

    }

    @Override
    public void onFavoritedRetweet(User arg0, User arg1, Status arg2) {

    }

    @Override
    public void onQuotedTweet(User arg0, User arg1, Status arg2) {

    }
}
