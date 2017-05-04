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

import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.mode.DocumentMode;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.location.structs.LocSched;

import java.util.*;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class LocationExtractionScheduler implements ILocationExtractionScheduler {

    private final DocumentMode opMode;
    private final ILocationRepository repos;
    private final ILocationExtractor locExtractor;
    private final ILocationExtractor entExtractor;
    private final IPolygonExtraction poly;
    private final ILocConf conf;

    public LocationExtractionScheduler(DocumentMode opMode, ILocationRepository repo, ILocationExtractor locExt,
                                       ILocationExtractor entExtractor, IPolygonExtraction pol, ILocConf conf) {
        this.opMode = opMode;
        this.repos = repo;
        this.locExtractor = locExt;
        this.entExtractor = entExtractor;
        this.poly = pol;
        this.conf = conf;
    }

    @Override
    public void executeTargetedUpdate()
    {
        // perform LE on a list of documents
        String documentListFile = conf.getDocumentListFile();
        if(documentListFile.isEmpty())
        {
            System.err.println("Did not supply document list file parameter.");
            return;
        }
        ArrayList<String> documentIDs = Utils.readFileLinesDropComments(documentListFile);
        if(documentIDs == null)
        {
            System.err.println("Failed to read the document ids file.");
            return;
        }
        Collection<Map<String, Object>> items =  new ArrayList<>();
        if(opMode == DocumentMode.ARTICLES) {
            for (String id : documentIDs)
                items.add(repos.loadArticle(id));
        }
        else if(opMode == DocumentMode.ARTICLES)
        {
            for (String id : documentIDs)
                items.add(repos.loadTweet(Long.parseLong(id)));
        }
        else if(opMode == DocumentMode.TEXT)
        {
            for (String id : documentIDs)
            {
                Map<String,Object> m = new HashMap<String,Object>();
                m.put("text",id);
                items.add(m);
            }

        }
        else
        {
            System.err.println("Invalid document mode : " + opMode.toString() +  " for retrieval mode " + conf.getDocumentRetrievalMode());
            System.err.println("Use articles or tweets , but not both");
            return;
        }

        Map<String,Set<String>> ids_entities = new HashMap<>();
        Map<String,Map<String,String>> id_geometries_map = new HashMap<>();


        if(conf.shouldExtractLocations(conf.getExtractionObjective()))
        {
            performExtraction(opMode,"locations", items, id_geometries_map,ids_entities);
            insertLocationData(opMode, id_geometries_map);

        }
        else if(conf.shouldExtractEntities(conf.getExtractionObjective()))
        {
            performExtraction(opMode, "entities", items,id_geometries_map,ids_entities);
            insertEntityData(opMode, ids_entities);
        }
        System.out.println("Targeted location extraction completed.");
    }
    @Override
    public void executeSchedule() {
        if (opMode == DocumentMode.BOTH) {
            DocumentMode[] opModeVals = DocumentMode.values();
            // iterate on the two first modes (tweets, articles)
            for (int i = 0; i < opModeVals.length - 1; i++) {
                DocumentMode m = opModeVals[i];
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

    private void executeSchedule(DocumentMode mode) {


        System.out.println("Executing schedule [" + mode.toString() + "]");
        LocSched schedLoc, schedEnt;
        // register starting operation
        String extractionObjective = conf.getExtractionObjective();
        boolean extractEntities = conf.shouldExtractEntities(extractionObjective) || extractionObjective.equals("all");
        boolean extractLocations = conf.shouldExtractLocations(extractionObjective);
        schedEnt = repos.scheduleInitialized(mode, "entities", Utils.getCalendarFromStringTimeWindow(conf.getDocumentRetrievalTimeWindow()));
        schedLoc = repos.scheduleInitialized(mode, "locations", Utils.getCalendarFromStringTimeWindow(conf.getDocumentRetrievalTimeWindow()));
        System.out.println("last parsed loc: " + new Date(schedLoc.getLastParsed()).toString());
        System.out.println("last parsed ent: " + new Date(schedEnt.getLastParsed()).toString());
        Collection<Map<String, Object>> itemsLoc = null;
        Collection<Map<String, Object>> itemsEnt = null;
        // npit switched loadArticles/loadTweets to loadAllArticles/loadAllTweets
        switch (mode) {
            case ARTICLES:
                // load items to process from last_parsed indicator.
                if(extractLocations)
                    itemsLoc = repos.loadArticles(schedLoc.getLastParsed()); // TODO
                if(extractEntities)
                    itemsEnt = repos.loadArticles(schedEnt.getLastParsed()); // TODO
                //System.err.println("*****Suspending article resuming - loading ALL for debuggery.");
                //items = repos.loadAllArticles(-1);
                break;
            case TWEETS:
                if(extractLocations)
                    itemsLoc = repos.loadTweets(schedLoc.getLastParsed());
                if(extractEntities)
                    itemsEnt = repos.loadTweets(schedEnt.getLastParsed());
                //System.err.println("*****Suspending twitter resuming - loading ALL for debuggery.");
                //items = repos.loadAllTweets(-1);
                break;
        }
        ExecRes er;
        // get location
        Map<String,Map<String,String>> id_geometries_map = new HashMap<>();
        Map<String,Set<String>> ids_entities = new HashMap<>();
        if(extractLocations) {
            er = performExtraction(mode,"locations", itemsLoc, id_geometries_map, ids_entities);
            insertLocationData(mode, id_geometries_map);
            schedLoc.setItemsUpdated(er.getItemsFound());
            schedLoc.setLastParsed(er.getMaxPublished());
            System.out.println("Set last parsed (locations) to " + schedLoc.getLastParsed());
            repos.scheduleFinalized(schedLoc,"locations");
        }
        if(extractEntities)
        {
            er = performExtraction(mode,"entities", itemsEnt, id_geometries_map,ids_entities);
            insertEntityData(mode,ids_entities);
            schedEnt.setItemsUpdated(er.getItemsFound());
            schedEnt.setLastParsed(er.getMaxPublished());
            System.out.println("Set last parsed (entities) to " + schedEnt.getLastParsed());
            repos.scheduleFinalized(schedEnt, "entities");
        }


        // update last timestamp parsed


        // register completed

    }

    private ExecRes performExtraction(DocumentMode mode, String extractionObjective, Collection<Map<String, Object>> items, Map<String,Map<String,String>> ids_geometries, Map<String,Set<String>> ids_entities) {

        // keep most recent published for reference
        long max_published = Long.MIN_VALUE;
        System.out.println("Initial max published: " + max_published);
        int i = 0;
        int count = 0;
        int resultsCount = 0;
        switch (mode) {
            case ARTICLES:
                if(conf.shouldExtractLocations(extractionObjective)) poly.init();
                ArrayList<String> permalinks = new ArrayList<>();

                // for each article
                for (Map<String, Object> article : items) {
                    if(article.isEmpty())
                    {
                        System.out.println("\tArticle " + ++count +  "/" +  items.size() + " is empty, skipping."); //debugprint
                        continue;
                    }
                    ++count;
                    String permalink;
                    long published = (long) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
                    max_published = Math.max(max_published, published);
                    permalink = (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
                    String clean_text = (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
                    System.out.print("\tArticle " + count +  "/" +  items.size() + " : "  + permalink); //debugprint
                    String RequiredResource;



                    if(conf.shouldExtractLocations(extractionObjective)) {
                        RequiredResource = locExtractor.ChooseRequiredResource(permalink, clean_text);
                        Set<String> locationsFound = locExtractor.doExtraction(RequiredResource);
                        if (!locationsFound.isEmpty()) {
                            Map<String, String> places_polygons = poly.extractPolygon(locationsFound);

                            // update entry
                            // edit geometry
                            places_polygons = poly.postProcessGeometries(places_polygons);

                            ids_geometries.put(permalink,places_polygons);
                            System.out.print(String.format(" %s", places_polygons.keySet().toString()));
                            i++;

                        }
                        else
                        {
                            ids_geometries.put(permalink,new HashMap<String,String>());
                            resultsCount++;
                        }

                    }
                    if(conf.shouldExtractEntities(extractionObjective)) {
                        RequiredResource = entExtractor.ChooseRequiredResource(permalink, clean_text);
                        Set<String> entitiesFound = entExtractor.doExtraction(RequiredResource);
                        ids_entities.put(permalink, new HashSet(entitiesFound));
                        if(! entitiesFound.isEmpty()) System.out.print(" \t" +entitiesFound);
                    }


                    System.out.println();
                    permalinks.add(permalink);

                }
                if(conf.shouldExtractLocations(extractionObjective)) {
                    System.out.println("\tLocation literal found for " + (items.size() - resultsCount) + " / " + items.size() + " articles.");
                    System.out.println("\t\tPolygon fetch failed for locations: " + poly.getFailedExtractionNames());
                }
                //repos.updateEventsWithAllLocationPolygonPairs(mode, null, null,article_geometries,permalinks);

                break;
            case TWEETS:
                if(conf.shouldExtractLocations(extractionObjective)) poly.init();
                // for each tweet
                for (Map<String, Object> item : items) {
                    ++count;
                    long published = (long) item.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_CREATED_AT.getColumnName());
                    max_published = Math.max(max_published, published);
                    long post_id = (long) item.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_POST_ID.getColumnName());
                    String tweet = (String) item.get(Cassandra.Twitter.TBL_TWITTER_POSTS_PER_DATE.FLD_TWEET.getColumnName());
                    String post_id_str = Long.toString(post_id);
                    String clean_tweet = Utils.cleanTweet(tweet);
                    System.out.print("\n\tTweet " + count +  "/" +  items.size() + " : "  + post_id); //debugprint


                    if(conf.shouldExtractLocations(extractionObjective)) {
                        if(!locExtractor.canHandleResource(ILocationExtractor.LE_RESOURCE_TYPE.TEXT)) break;
                        Set<String> locationsFound = locExtractor.doExtraction(clean_tweet);
                        if (!locationsFound.isEmpty()) {
                            Map<String, String> places_polygons = poly.extractPolygon(locationsFound);
                            places_polygons = poly.postProcessGeometries(places_polygons);

                            ids_geometries.put(post_id_str ,places_polygons);
                            if(! places_polygons.keySet().isEmpty())
                                System.out.print(String.format(" %s", places_polygons.keySet().toString()));

                            i++;
                        }
                        else {
                            resultsCount++;
                            ids_geometries.put(post_id_str ,new HashMap<String,String>());
                        }
                    }
                    if(conf.shouldExtractEntities(extractionObjective)) {
                        if(!entExtractor.canHandleResource(ILocationExtractor.LE_RESOURCE_TYPE.TEXT)) break;
                        Set<String> entitiesFound = entExtractor.doExtraction(clean_tweet);
                        ids_entities.put(post_id_str, new HashSet(entitiesFound));
                        if(! entitiesFound.isEmpty()) System.out.print(" \t" +entitiesFound);
                    }
                }
                if(conf.shouldExtractLocations(extractionObjective)) {
                    System.out.println("\tLocation literal found for " + (items.size() - resultsCount) + " / " + items.size() + " tweets ");
                    System.out.println("\t\tPolygon fetch failed for locations: " + poly.getFailedExtractionNames());
                }
                //repos.updateEventsWithAllLocationPolygonPairs(mode, tweet_geometries, post_ids,null, null);

                break;
            case TEXT:
                if(conf.shouldExtractLocations(extractionObjective)) poly.init();
                for (Map<String, Object> item : items) {
                    String text = (String) item.get("text");
                    String textid=text.substring(0,30);
                    String textprint = "";
                    if(conf.hasModifier(IBaseConf.Modifiers.VERBOSE.toString())) textprint=text;
                    else textprint = textid + "[...] ";
                    System.out.print("\tText " + ++count +  "/" +  items.size() + " : "  + textprint);

                    if(conf.shouldExtractLocations(extractionObjective)) {
                        if(!locExtractor.canHandleResource(ILocationExtractor.LE_RESOURCE_TYPE.TEXT)) break;
                        Set<String> locationsFound = locExtractor.doExtraction(text);
                        if (!locationsFound.isEmpty()) {
                            Map<String, String> places_polygons = poly.extractPolygon(locationsFound);
                            places_polygons = poly.postProcessGeometries(places_polygons);
                            ids_geometries.put(textid,places_polygons);
                            i++;
                            if(! places_polygons.keySet().isEmpty())
                                System.out.println(String.format(" %s", places_polygons.keySet().toString()));

                        }
                        else {
                            resultsCount++;
                            ids_geometries.put("",new HashMap<String,String>());
                            System.out.println("");
                        }
                    }
                    if(conf.shouldExtractEntities(extractionObjective)) {
                        if(!entExtractor.canHandleResource(ILocationExtractor.LE_RESOURCE_TYPE.TEXT)) break;
                        Set<String> entitiesFound = entExtractor.doExtraction(text);
                        ids_entities.put(textid, new HashSet(entitiesFound));
                        if(! entitiesFound.isEmpty()) System.out.print(" \t" +entitiesFound);
                    }


                }
                if(conf.shouldExtractLocations(extractionObjective)) {
                    System.out.println("\tLocation literal found for " + (items.size() - resultsCount) + " / " + items.size() + " texts ");
                    System.out.println("\t\tPolygon fetch failed for locations: " + poly.getFailedExtractionNames());
                }

        }
        return new ExecRes(max_published, i);
    }

    private void insertLocationData(DocumentMode mode, Map<String,Map<String,String>> ids_geometries_entities)
    {
        switch (mode) {
            case ARTICLES:
                repos.updateArticlesWithReferredPlaceMetadata(ids_geometries_entities);

                break;
            case TWEETS:
                // update entry (tweets_per_referred_place)
                repos.updateTweetsWithReferredPlaceMetadata(ids_geometries_entities);
                break;
        }

    }
    private void insertEntityData(DocumentMode mode, Map<String,Set<String>> ids_entities)
    {
        switch (mode) {
            case ARTICLES:
                repos.updateArticlesWithEntities(ids_entities);

                break;
            case TWEETS:
                // update entry (tweets_per_referred_place)
                repos.updateTweetsWithEntities(ids_entities);
                break;
        }

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
