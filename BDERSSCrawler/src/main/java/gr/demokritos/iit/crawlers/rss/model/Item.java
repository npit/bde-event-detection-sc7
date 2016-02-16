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

public class Item {
    private final String feedUrl;
    private final CrawlId crawlId;

    public Item(String feedUrl, CrawlId crawlId) {
        this.feedUrl = feedUrl;
        this.crawlId = crawlId;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public CrawlId getCrawlId() {
        return crawlId;
    }

    @Override
    public String toString() {
        return "Item<<" + crawlId + ">, <" + feedUrl + ">>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        if (crawlId != null ? !crawlId.equals(item.crawlId) : item.crawlId != null) return false;
        if (feedUrl != null ? !feedUrl.equals(item.feedUrl) : item.feedUrl != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = feedUrl != null ? feedUrl.hashCode() : 0;
        result = 31 * result + (crawlId != null ? crawlId.hashCode() : 0);
        return result;
    }
}
