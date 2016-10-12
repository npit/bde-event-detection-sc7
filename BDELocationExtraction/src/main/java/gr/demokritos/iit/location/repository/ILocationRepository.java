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
package gr.demokritos.iit.location.repository;

import gr.demokritos.iit.base.repository.IBaseRepository;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.structs.LocSched;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocationRepository extends IBaseRepository {

    void doHotfix();

    void storeAndChangeDetectionEvents(String strabonURL);
    /**
     *
     * @param permalink the URL of the article
     * @param places_polygons the places found for this article
     */
    void updateArticlesWithReferredPlaceMetadata(String permalink, Map<String, String> places_polygons);

    /**
     *
     * @param post_id the unique ID of the tweet
     * @param places_polygons the places that this tweet found to refer to
     */
    void updateTweetsWithReferredPlaceMetadata(long post_id, Map<String, String> places_polygons);

    /**
     *
     * @return the timestamp of the last article parsed in the previous
     * execution
     */
    LocSched scheduleInitialized(OperationMode mode);

    /**
     * register schedule completed
     *
     * @param schedule
     */
    void scheduleFinalized(LocSched schedule);

    Map<String, Object> loadArticlePerPlace(String place_literal, String entry_url);

    void updateEventsWithArticleLocationPolygonPairs(Map<String,String> places_polygons, String permalink);
    void updateEventsWithTweetLocationPolygonPairs(Map<String,String> places_polygons, long post_id);
    void updateEventsWithAllLocationPolygonPairs(OperationMode mode, ArrayList<Map<String,String>> tweet_places_polygons, ArrayList<Long> post_ids, ArrayList<Map<String,String>> article_places_polygons, ArrayList<String> permalinks);

    void onlyUpdateEventsWithLocationInformation();
}
