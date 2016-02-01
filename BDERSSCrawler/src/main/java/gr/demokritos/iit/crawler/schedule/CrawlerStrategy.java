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
package gr.demokritos.iit.crawler.schedule;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import gr.demokritos.iit.crawler.SimpleExtractor;

public interface CrawlerStrategy {

    public CrawlerStrategy BLOG = new CrawlerStrategy() {

        @Override
        public String crawlerType() {
            return "blog";
        }

        @Override
        public BoilerpipeExtractor extractor() {
            return new SimpleExtractor();
        }
    };

    public CrawlerStrategy NEWS = new CrawlerStrategy() {

        @Override
        public String crawlerType() {
            return "news";
        }

        @Override
        public BoilerpipeExtractor extractor() {
            return new ArticleExtractor();
        }
    };

    String crawlerType();

    BoilerpipeExtractor extractor();
}
