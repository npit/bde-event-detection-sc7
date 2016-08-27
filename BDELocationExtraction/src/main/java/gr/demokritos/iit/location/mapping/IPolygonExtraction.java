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
package gr.demokritos.iit.location.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IPolygonExtraction {

    /**
     * extract polygon for a location entry
     *
     * @param locationEntity
     * @return
     */
    String extractPolygon(String locationEntity);

    /**
     * extract polygon for a series of entries
     *
     * @param locationEntities
     * @return
     */
    Map<String, String> extractPolygon(Collection<String> locationEntities);

    Map<String, String> parseGeomJSON(Map<String, String> input);

}
