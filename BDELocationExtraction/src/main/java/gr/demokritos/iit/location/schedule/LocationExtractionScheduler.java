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
package gr.demokritos.iit.location.schedule;

import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.location.structs.LocSched;
import gr.demokritos.iit.location.util.GeometryFormatTransformer;

import java.util.*;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class LocationExtractionScheduler implements ILocationExtractionScheduler {

    private final OperationMode opMode;
    private final ILocationRepository repos;
    private final ILocationExtractor locExtractor;
    private final IPolygonExtraction poly;
    private final ILocConf conf;

    public LocationExtractionScheduler(OperationMode opMode, ILocationRepository repo, ILocationExtractor locExt, IPolygonExtraction pol, ILocConf conf) {
        this.opMode = opMode;
        this.repos = repo;
        this.locExtractor = locExt;
        this.poly = pol;
        this.conf = conf;
    }

    @Override
    public void executeSchedule() {
        if (opMode == OperationMode.BOTH) {
            OperationMode[] opModeVals = OperationMode.values();
            // iterate on the two first modes (tweets, articles)
            for (int i = 0; i < opModeVals.length - 1; i++) {
                OperationMode m = opModeVals[i];
                // execute schedule for current mode
                executeSchedule(m);

            }
            //TODO popeye process call!
            // call popeye.di.uoa.gr - process
            // repos.storeAndChangeDetectionEvents();
        } else {
            executeSchedule(opMode);
        }
    }

    private void executeSchedule(OperationMode mode) {



        LocSched sched;
        // register starting operation
        sched = repos.scheduleInitialized(mode,Utils.getCalendarFromStringTimeWindow(conf.getDocumentRetrievalTimeWindow()));
        System.out.println("last parsed: " + new Date(sched.getLastParsed()).toString());
        Collection<Map<String, Object>> items = null;
        // npit switched loadArticles/loadTweets to loadAllArticles/loadAllTweets
        switch (mode) {
            case ARTICLES:
                // load items to process from last_parsed indicator.
                items = repos.loadArticles(sched.getLastParsed()); // TODO
                //System.err.println("*****Suspending article resuming - loading ALL for debuggery.");
                //items = repos.loadAllArticles(-1);
                break;
            case TWEETS:
                items = repos.loadTweets(sched.getLastParsed());
                //System.err.println("*****Suspending twitter resuming - loading ALL for debuggery.");
                //items = repos.loadAllTweets(-1);
                break;
        }
        ExecRes er;
        // get location
        er = extractLocation(items, mode);
        // schedule updated
        sched.setItemsUpdated(er.getItemsFound());
        // update last timestamp parsed

        sched.setLastParsed(er.getMaxPublished());
        System.out.println("Set last parsed to " + sched.getLastParsed());
        // register completed
        repos.scheduleFinalized(sched);
    }

    private ExecRes extractLocation(Collection<Map<String, Object>> items, OperationMode mode) {

        // keep most recent published for reference

        long max_published = Long.MIN_VALUE;
        System.out.println("Initial max published: " + max_published);
        int i = 0;
        int count = 0;
        int noLocationCount = 0;
        switch (mode) {
            case ARTICLES:
                poly.init();
                ArrayList<String> permalinks = new ArrayList<>();
                ArrayList<Map<String,String>> article_geometries = new ArrayList<>();
                // for each article
                for (Map<String, Object> article : items) {

                    ++count;
                    String permalink;
                    long published = (long) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
                    max_published = Math.max(max_published, published);

                    permalink = (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
                    String clean_text = (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
                    // extract location entities
                    //System.out.println("Extracting location for article " + permalink);

                    Set<String> locationsFound = locExtractor.extractLocation(clean_text);
                    System.out.print("\tArticle " + count +  "/" +  items.size() + " : "  + permalink); //debugprint

                    if (!locationsFound.isEmpty()) {
                        Map<String, String> places_polygons = poly.extractPolygon(locationsFound);
                        // update entry
                        // edit geometry
                        places_polygons = poly.postProcessGeometries(places_polygons);

                        repos.updateArticlesWithReferredPlaceMetadata(permalink, places_polygons);
                        article_geometries.add(places_polygons);

                        i++;
                    }
                    else
                    {
                        article_geometries.add(new HashMap<String,String>());
                        noLocationCount++;
                        System.out.println(" - no location found");
                    }
                    permalinks.add(permalink);

                }
                System.out.println("\tLocation literal found for " + (items.size() - noLocationCount) + " / " + items.size() + " articles.");
                System.out.println("\t\tPolygon fetch failed for locations: " + poly.getFailedExtractionNames());
                //repos.updateEventsWithAllLocationPolygonPairs(mode, null, null,article_geometries,permalinks);

                break;
            case TWEETS:
                poly.init();
                ArrayList<Long> post_ids = new ArrayList<>();
                ArrayList<Map<String,String>> tweet_geometries = new ArrayList<>();
                // for each tweet
                for (Map<String, Object> item : items) {
                    ++count;
                    long published = (long) item.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName());
                    max_published = Math.max(max_published, published);

                    long post_id = (long) item.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName());
                    String tweet = (String) item.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName());
                    // clean tweet
                    String clean_tweet = Utils.cleanTweet(tweet);
                    // extract location entities
                    //System.out.println("Extracting location for tweet " + post_id);
                    Set<String> locationsFound = locExtractor.extractLocation(clean_tweet);
                    // extract coordinates for each entity
                    System.out.print("\tTweet " + count +  "/" +  items.size() + " : "  + post_id); //debugprint

                    if (!locationsFound.isEmpty()) {
                        Map<String, String> places_polygons = poly.extractPolygon(locationsFound);
                        places_polygons = poly.postProcessGeometries(places_polygons);
                        // update entry (tweets_per_referred_place)
                        repos.updateTweetsWithReferredPlaceMetadata(post_id, places_polygons);
                        tweet_geometries.add(places_polygons);

                        i++;
                    }
                    else {
                        noLocationCount++;
                        tweet_geometries.add(new HashMap<String, String>());
                        System.out.println(" - no location found");
                    }
                    post_ids.add(post_id);
                }
                System.out.println("\tLocation literal found for " + (items.size() - noLocationCount)  + " / " + items.size() + " tweets ");
                System.out.println("\t\tPolygon fetch failed for locations: " + poly.getFailedExtractionNames());
                //repos.updateEventsWithAllLocationPolygonPairs(mode, tweet_geometries, post_ids,null, null);

                break;
        }
        return new ExecRes(max_published, i);
    }



    /**
     * holds the last_updated timestamp, and items_parsed values
     */
    private class ExecRes {
        private final long max_published;
        private final int items_found;

        public ExecRes(long max_published, int items_found) {
            this.max_published = max_published;
            this.items_found = items_found;
        }
        public int getItemsFound() {
            return items_found;
        }
        public long getMaxPublished() {
            return max_published;
        }
    }
}
