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
package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.location.factory.conf.ILocConf;

import java.util.List;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocationExtractor {

    /**
     * extract location entities from text.
     *
     * @param document
     * @return
     */
    // what part of the article the location extractor requires, i.e. the text or the url, etc
    LE_RESOURCE_TYPE getRequiredResource();
    enum LE_RESOURCE_TYPE
    {
        URL("url"), TEXT("text");
        LE_RESOURCE_TYPE(String s)
        {
            name = s;
        }

        public String getName() {
            return name;
        }

        private final String name;
        @Override
        public String toString()
        {
            return name;
        }
    }
    List<String> doExtraction(String resource);
    boolean configure(ILocConf conf);
    String ChooseRequiredResource(String res1, String res2);
    boolean canHandleResource(ILocationExtractor.LE_RESOURCE_TYPE res);
}
