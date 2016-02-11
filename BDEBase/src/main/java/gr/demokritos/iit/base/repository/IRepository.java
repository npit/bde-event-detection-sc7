/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.base.repository;

import java.util.Collection;
import java.util.Map;

/**
 * load data from backend
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IRepository {

    /**
     * load tweets that occured after a specified timestamp
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
    
    
}
