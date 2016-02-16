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

public class CrawlId {
    private final long crawlId;
    private final long startTimestamp;
    private long endTimestamp;

    public CrawlId(long crawlId) {
        this.crawlId = crawlId;
        this.startTimestamp = System.currentTimeMillis();

        //negative timestamp means this crawl has not finished
        this.endTimestamp = -1;
    }

    public CrawlId(long crawlId, long startTimestamp, long endTimestamp) {
        this.crawlId = crawlId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    public long getId() {
        return crawlId;
    }

    @Override
    public String toString() {
        return "CrawlId<" + crawlId + ", " + startTimestamp + ", " + endTimestamp +">";
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void finishedCrawling() {
        endTimestamp = System.currentTimeMillis();
    }

    public boolean hasFinishedCrawling() {
        return endTimestamp > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrawlId crawlId1 = (CrawlId) o;

        if (crawlId != crawlId1.crawlId) return false;
        if (endTimestamp != crawlId1.endTimestamp) return false;
        if (startTimestamp != crawlId1.startTimestamp) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (crawlId ^ (crawlId >>> 32));
        result = 31 * result + (int) (startTimestamp ^ (startTimestamp >>> 32));
        result = 31 * result + (int) (endTimestamp ^ (endTimestamp >>> 32));
        return result;
    }

}
