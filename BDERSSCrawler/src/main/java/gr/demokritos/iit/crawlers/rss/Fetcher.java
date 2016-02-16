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
package gr.demokritos.iit.crawlers.rss;

import gr.demokritos.iit.crawlers.rss.model.Content;
import java.io.IOException;

/**
 * User: ade
 */
public interface Fetcher {

    public final String USER_AGENT = "BigDataEurope/0.1 (http://www.big-data-europe.eu/)";

    /**
     * Fetch the given URL and return a Content object. If the url hasn't
     * changed (due to etag and last-modified) or returns a 404 status then
     * return null;
     *
     * @param url
     * @return
     * @throws IOException
     */
    public Content fetchUrl(String url) throws IOException;
}
