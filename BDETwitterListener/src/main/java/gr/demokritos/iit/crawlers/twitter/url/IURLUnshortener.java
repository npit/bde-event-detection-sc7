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
package gr.demokritos.iit.crawlers.twitter.url;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IURLUnshortener {

    int DEFAULT_CACHE_SIZE = 10000;
    int DEFAULT_CONNECT_TIMEOUT = 2000;
    int DEFAULT_READ_TIMEOUT = 2000;
    int DEFAULT_HARD_TIMEOUT = 5000;

    /**
     * Will try to expand the shortened URL as many times as needed to reach the
     * final endpoint. Will return the provided shortened URL on fail
     *
     * @param urlArg the shortened URL
     * @return the full URL
     */
    String expand(String urlArg);

}
