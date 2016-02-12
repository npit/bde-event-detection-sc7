/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.repository;

import gr.demokritos.iit.base.repository.IBaseRepository;
import gr.demokritos.iit.structs.LocSched;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocationRepository extends IBaseRepository {

    void updateArticleWithPlacesLiteral(String permalink, Set<String> places);

    /**
     *
     * @return the timestamp of the last article parsed in the previous
     * execution
     */
    LocSched scheduleInitialized();

    /**
     * register schedule completed
     *
     * @param schedule
     */
    void scheduleFinalized(LocSched schedule);

}
