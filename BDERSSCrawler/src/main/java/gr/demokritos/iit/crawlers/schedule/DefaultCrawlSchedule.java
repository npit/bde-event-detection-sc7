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
package gr.demokritos.iit.crawlers.schedule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultCrawlSchedule implements CrawlSchedule {

    private final ScheduleLoader loader;
    private final DomainExtractor extractor;
    private final Map<String, List<String>> domainsToUrlsMap;
    private Iterator<String> urls;

    public DefaultCrawlSchedule(ScheduleLoader loader, DomainExtractor extractor) {
        this.loader = loader;
        this.extractor = extractor;
        domainsToUrlsMap = Maps.newHashMap();
    }

    @Override
    public String nextUrl() {
        if (needsLoading()) {
            this.load();
        }

        if (urls.hasNext()) {
            return urls.next();
        }
        urls = null;

        return null;
    }

    private boolean needsLoading() {
        return urls == null;
    }

    @Override
    public void load() {
        List<String> urlList = loader.load();
        domainsToUrlsMap.clear();

        //Use our own set rather than the map's keyset because we'll need to modify it whilst iterating over the map.
        //We also need to preserve the original insertion order
        Set<String> domains = Sets.newLinkedHashSet();
        for (String url : urlList) {
            String domain = extractor.extractDomain(url);
            domains.add(domain);
            if (!domainsToUrlsMap.containsKey(domain)) {
                List<String> urlsForDomain = Lists.newArrayList(url);
                domainsToUrlsMap.put(domain, urlsForDomain);
            } else {
                List<String> urlsForDomain = domainsToUrlsMap.get(domain);
                urlsForDomain.add(url);
            }
        }

        List<String> domainOrderedUrls = Lists.newArrayList();

        int level = 0;
        while (!domainsToUrlsMap.isEmpty()) {
            for (String domain : domains) {
                if (!domainsToUrlsMap.containsKey(domain)) {
                    continue;
                }

                List<String> domainUrls = domainsToUrlsMap.get(domain);
                if (domainUrls.size() == level) {
                    domainsToUrlsMap.remove(domain);
                } else {
                    domainOrderedUrls.add(domainUrls.get(level));
                }
            }
            level++;
        }
        urls = domainOrderedUrls.iterator();
    }
}
