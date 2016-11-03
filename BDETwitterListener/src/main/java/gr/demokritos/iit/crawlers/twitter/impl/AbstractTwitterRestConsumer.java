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
package gr.demokritos.iit.crawlers.twitter.impl;

import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.crawlers.twitter.factory.conf.ITwitterConf;
import static gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory.LOGGER;
import gr.demokritos.iit.crawlers.twitter.policy.DefensiveCrawlPolicy;
import gr.demokritos.iit.crawlers.twitter.policy.ICrawlPolicy;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;
import gr.demokritos.iit.crawlers.twitter.utils.QueryLoader;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import javax.xml.transform.Source;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public abstract class AbstractTwitterRestConsumer {

    protected final String twitterConsumerKey;
    protected final String twitterConsumerKeySecret;
    protected final String twitterAccessTokken;
    protected final String twitterAccessTokkenSecret;

    protected IRepository repository;

    protected static final String TWITTER_API_CALL_USER_TIMELINE = "/statuses/user_timeline";
    protected static final String GEOCODING_API_CLIENT_NAME = "bdedevcrawl";

    // keep the config around
    protected final ITwitterConf config;
    // will extract URLs from the tweet, if any
//    protected Extractor extractor;
    // twitter-text lib is a maven snapshot build at 16/05/14
    // cloned from https://github.com/twitter/twitter-text-java.git
    protected Twitter twitter;
    protected ICrawlPolicy policy;

    /**
     * Main constructor. Accepts a configuration class that has already read
     * resources. Default CrawlingPolicy=Defensive
     *
     * @param config the configuration class
     * @param repository
     */
    public AbstractTwitterRestConsumer(ITwitterConf config, IRepository repository) {
        // init credentials
        this.twitterConsumerKey = config.getTwitterConsumerKey();
        this.twitterConsumerKeySecret = config.getTwitterConsumerKeySecret();
        this.twitterAccessTokken = config.getTwitterAccessTokken();
        this.twitterAccessTokkenSecret = config.getTwitterAccessTokkenSecret();
        this.repository = repository;
        this.policy = new DefensiveCrawlPolicy(repository);
        //connect
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(twitterConsumerKey)
                .setOAuthConsumerSecret(twitterConsumerKeySecret)
                .setOAuthAccessToken(twitterAccessTokken)
                .setOAuthAccessTokenSecret(twitterAccessTokkenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        // get active instance
        this.twitter = tf.getInstance();
        this.config = config;
    }

    /**
     * Alternate constructor. Accepts a configuration class that has already
     * read resources.
     *
     * @param config the configuration class
     * @param repository
     * @param policy
     */
    public AbstractTwitterRestConsumer(ITwitterConf config, IRepository repository, ICrawlPolicy policy) {
        // init credentials
        this.twitterConsumerKey = config.getTwitterConsumerKey();
        this.twitterConsumerKeySecret = config.getTwitterConsumerKeySecret();
        this.twitterAccessTokken = config.getTwitterAccessTokken();
        this.twitterAccessTokkenSecret = config.getTwitterAccessTokkenSecret();
        this.repository = repository;
        this.policy = policy;
        //connect
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(twitterConsumerKey)
                .setOAuthConsumerSecret(twitterConsumerKeySecret)
                .setOAuthAccessToken(twitterAccessTokken)
                .setOAuthAccessTokenSecret(twitterAccessTokkenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        // get active instance
        this.twitter = tf.getInstance();
        this.config = config;

    }

    protected List<Status> processStatuses(List<Status> statuses, IRepository.CrawlEngine engine_type, long engine_id) {
        List<Status> res = new ArrayList();
        // for each status
        for (Status status : statuses) {
            // if it is a retweet, get the original tweet
            while (status.isRetweet()) {
                status = status.getRetweetedStatus();
            }
            // add status to result list (avoid retweets)
            res.add(status);
            // proceed with storing in twitter repository
            long postID = status.getId();
            User user = status.getUser();
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
                repository.insertPost(status, userID, user.getScreenName(), followersWhenPublished, engine_type, engine_id);
            }
        }
        return res;
    }

    public Collection<SourceAccount> getAccounts(ITwitterConf conf)
    {
        Collection<SourceAccount> accounts=null;
        String accountsSourceMode = conf.getAccountsSourceMode();
        if (accountsSourceMode.isEmpty()) return null;
        String accountsSource = conf.getAccountsSource();
        System.out.println(String.format("Got accounts source mode [%s]",accountsSourceMode));
        System.out.println(String.format("Got accounts source  [%s]",accountsSource));
        if(accountsSourceMode.equals(IBaseConf.SourceMode.LOCAL.toString()))
        {
            // read from file
            try {
                accounts = QueryLoader.loadAccounts(accountsSource,"[*]{3}",false);
            } catch (IOException e) {
                e.printStackTrace();
                return accounts;
            }
            LOGGER.info(String.format("Fetched %d accounts from the local file.", (accounts.size())));

        }
        else if(accountsSourceMode.equals(IBaseConf.SourceMode.REMOTE.toString()))
        {
            // get remotely
            LOGGER.info(String.format("Fetched %d accounts from the remote source.", (accounts.size())));

        }
        else if(accountsSourceMode.equals(IBaseConf.SourceMode.REPOSITORY.toString()))
        {
            accounts = repository.getAccounts();
            LOGGER.info(String.format("Fetched %d accounts from the repository.", (accounts.size())));

        }
        return accounts;
    }
    /**
     * get the statistics of the crawler, for rate limit issues
     *
     * @param sKey the REST call to check
     * @return results from rate limit status API call
     */
    protected Map<String, Integer> getRateLimitStatus(String sKey) {
        Map<String, Integer> res = new HashMap();
        Map<String, RateLimitStatus> rateLimitStatus;
        try {
            rateLimitStatus = twitter.getRateLimitStatus();
            RateLimitStatus value = rateLimitStatus.get(sKey);
            res.put(API_RATE_LIMIT, value.getLimit());
            res.put(API_REMAINING_CALLS, value.getRemaining());
            res.put(API_SECONDS_UNTIL_RESET, value.getSecondsUntilReset());
        } catch (TwitterException ex) {
            LOGGER.warning(ex.getMessage());
        }
        return res;
    }

    protected boolean checkAPICallStatus(int counter, int remaining_calls_before_limit, long time_started, long seconds_until_reset_from_start) throws InterruptedException {
        boolean reset = false;
        if (counter == 1 && remaining_calls_before_limit > 1) {
            return false;
        }
        // check for rate limit reached
        if (counter == remaining_calls_before_limit) {
            long ctime = Calendar.getInstance().getTimeInMillis();
            long elapsed_seconds = TimeUnit.SECONDS.convert((ctime - time_started), TimeUnit.MILLISECONDS);
            long seconds_diff = elapsed_seconds - seconds_until_reset_from_start;

            if (seconds_diff <= 0) {
                LOGGER.info(String.format("Reached Rate limit, will sleep for %d seconds to overcome", (-seconds_diff) + 1));
                Thread.sleep(TimeUnit.MILLISECONDS.convert((-seconds_diff) + 1, TimeUnit.SECONDS));
                reset = true;
            }
        }
        return reset;
    }

//    /**
//     * try to find coordinates of the place called
//     *
//     * @param place_literal
//     * @return
//     */
//    protected TGeoLoc extractGeolocationFromPlace(String place_literal) {
//        TGeoLoc gl = new TGeoLoc(0.0, 0.0);
//        if (geonames_client != null) {
//            try {
//                ToponymSearchResult coordinates = geonames_client.getCoordinates(place_literal);
//                if (coordinates.getTotalResultsCount() > 1) {
//                    List<Toponym> toponyms = coordinates.getToponyms();
//                    Toponym first = toponyms.get(0);
//                    gl.setLatitude(first.getLatitude());
//                    gl.setLongitude(first.getLongitude());
//                }
//            } catch (Exception ex) {
//                LOGGER.warning(ex.getMessage());
//            }
//        }
//        return gl;
//    }
//
//    /**
//     * find nearby places from the place
//     *
//     * @param latitude
//     * @param longitude
//     * @return
//     */
//    protected TPlace extractNearbyPlace(double latitude, double longitude) {
//        TPlace place = new TPlace();
//        if (geonames_client != null) {
//            try {
//                List<Toponym> findNearby = geonames_client.findNearby(latitude, longitude);
//                if (findNearby.size() > 1) {
//                    Toponym t = findNearby.get(0);
//                    if (t.getBoundingBox() != null) {
//                        place.setBoundingBoxType(t.getBoundingBox().toString());
//                    }
//                    if (t.getCountryName() != null) {
//                        place.setCountry(t.getCountryName());
//                    }
//                    if (t.getCountryCode() != null) {
//                        place.setCountryCode(t.getCountryCode());
//                    }
//                    if (t.getName() != null) {
//                        place.setName(t.getName());
//                    }
//                    if (t.getGeoNameId() != 0) {
//                        place.setId(String.valueOf(t.getGeoNameId()));
//                    }
//                }
//            } catch (Exception ex) {
//                LOGGER.warning(ex.getMessage());
//            }
//        }
//        return place;
//    }
    protected static final String API_RATE_LIMIT = "limit";
    protected static final String API_REMAINING_CALLS = "remaining_calls";
    protected static final String API_SECONDS_UNTIL_RESET = "seconds_until_reset";
}
