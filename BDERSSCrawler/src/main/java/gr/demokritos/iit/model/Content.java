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
package gr.demokritos.iit.model;

import java.util.Date;

/**
 * User: ade
 */
public class Content {

    private final String url;
    private final String rawText;
    private final String etag;
    private final String lastModified;
    private final Date crawledDate;

    public Content(String url, String rawText, String etag, String lastModified, Date crawledDate) {
        this.url = url.trim();
        this.rawText = rawText;
        this.etag = etag;
        this.lastModified = lastModified;
        this.crawledDate = crawledDate;
    }

    /**
     * the permalink of the article
     * @return 
     */
    public String getUrl() {
        return url;
    }

    public String getRawText() {
        return rawText;
    }

    public String getEtag() {
        return etag;
    }

    public String getLastModified() {
        return lastModified;
    }

    /**
     * Return the metadata associated with the content's URL.
     *
     * @return a UrlMetaData object containing the etag and lastModified seen on
     * the last request to this Content's URL.
     */
    public UrlMetaData getUrlMetaData() {
        return new UrlMetaData(etag, lastModified);
    }

    public Date getCrawlDate() {
        return crawledDate;
    }
}
