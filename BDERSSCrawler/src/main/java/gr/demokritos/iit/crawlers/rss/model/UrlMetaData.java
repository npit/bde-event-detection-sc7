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
package gr.demokritos.iit.crawlers.rss.model;

/**
 * User: ade
 */
public class UrlMetaData {

    private final String etag;
    private final String lastModified;

    public UrlMetaData(String etag, String lastModified) {
        this.etag = etag;
        this.lastModified = lastModified;
    }

    public String getEtag() {
        return etag;
    }

    public String getLastModified() {
        return lastModified;
    }

    @Override
    public String toString() {
        return "UrlMetaData<" + etag + ", " + lastModified + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UrlMetaData that = (UrlMetaData) o;

        if (etag != null ? !etag.equals(that.etag) : that.etag != null) {
            return false;
        }
        if (lastModified != null ? !lastModified.equals(that.lastModified) : that.lastModified != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = etag != null ? etag.hashCode() : 0;
        result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
        return result;
    }
}
