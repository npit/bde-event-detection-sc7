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
package gr.demokritos.iit.location.extraction.provider;

import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ITokenProvider {

    public String CHARSET_UTF8 = "UTF-8";

    /**
     * Provided a text, extract the Named Entities found, based on a preloaded
     * model stack if text is null or empty, return empty Set
     *
     * @param text
     * @return
     */
    Set<String> getTokens(String text);

    /**
     * extract named entities related to location
     *
     * @param text
     * @return
     */
    Set<String> getLocationTokens(String text);
}
