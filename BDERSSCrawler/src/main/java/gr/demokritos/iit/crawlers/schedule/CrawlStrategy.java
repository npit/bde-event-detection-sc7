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

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import gr.demokritos.iit.crawlers.extractors.SimpleExtractor;

public interface CrawlStrategy {

    String ARTICLES = "articles";
    String ARTICLES_PER_DATE = "articles_per_published_date";
    String ARTICLES_PER_PLACE = "articles_per_place";
    String FEEDS = "feeds";
    String CRAWLS = "crawls";

    public CrawlStrategy BLOG = new CrawlStrategy() {
        @Override
        public String crawlType() {
            return "blog";
        }

        @Override
        public BoilerpipeExtractor extractor() {
            return new SimpleExtractor();
        }

        @Override
        public String TableArticles() {
            return crawlType().concat("_").concat(ARTICLES);
        }

        @Override
        public String TableArticlesPerDate() {
            return crawlType().concat("_").concat(ARTICLES_PER_DATE);
        }

        @Override
        public String TableArticlesPerPlace() {
            return crawlType().concat("_").concat(ARTICLES_PER_PLACE);
        }

        @Override
        public String TableFeeds() {
            return crawlType().concat("_").concat(FEEDS);
        }

        @Override
        public String TableCrawls() {
            return crawlType().concat("_").concat(CRAWLS);
        }
    };

    public CrawlStrategy NEWS = new CrawlStrategy() {

        @Override
        public String crawlType() {
            return "news";
        }

        @Override
        public BoilerpipeExtractor extractor() {
            return new ArticleExtractor();
        }

        @Override
        public String TableArticles() {
            return crawlType().concat("_").concat(ARTICLES);
        }

        @Override
        public String TableArticlesPerDate() {
            return crawlType().concat("_").concat(ARTICLES_PER_DATE);
        }

        @Override
        public String TableArticlesPerPlace() {
            return crawlType().concat("_").concat(ARTICLES_PER_PLACE);
        }

        @Override
        public String TableFeeds() {
            return crawlType().concat("_").concat(FEEDS);
        }

        @Override
        public String TableCrawls() {
            return crawlType().concat("_").concat(CRAWLS);
        }
    };

    String crawlType();

    BoilerpipeExtractor extractor();

    String TableArticles();

    String TableArticlesPerDate();

    String TableArticlesPerPlace();

    String TableFeeds();

    String TableCrawls();
}
