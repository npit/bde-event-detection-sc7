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
package gr.demokritos.iit.base.repository;

import java.util.Collection;
import java.util.Map;

/**
 * load data from backend
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IBaseRepository {

    // npit : Added load All Tweets/Articles interface methods
    /**
     * load all tweets
     *
     * @param atmost
     * @return
     */
    Collection<Map<String, Object>> loadAllTweets(int atmost);


    /**
     * load all articles
     *
     * @param atmost
     * @return
     */
    Collection<Map<String, Object>> loadAllArticles(int atmost);



    /**
     * load tweets that occurred after a specified timestamp
     *
     * @param from
     * @return
     */
    Collection<Map<String, Object>> loadTweets(long from);

    /**
     *
     * @param from
     * @return
     */
    Collection<Map<String, Object>> loadArticles(long from);

    /**
     * load news_article by permalink key.
     *
     * @param permalink
     * @return
     */
    Map<String, Object> loadArticle(String permalink);

    /**
     * load tweet by post ID
     *
     * @param post_id the unique key of the tweet
     * @return
     */
    Map<String, Object> loadTweet(long post_id);


}
