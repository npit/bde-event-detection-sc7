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
package gr.demokritos.iit.crawlers;

import gr.demokritos.iit.crawlers.factory.SystemFactory;
import gr.demokritos.iit.crawlers.factory.RSSConf;
import gr.demokritos.iit.repository.IRepository;
import java.beans.PropertyVetoException;

public class BlogCrawler extends AbstractCrawler {

    public BlogCrawler(SystemFactory factory, RSSConf configuration) throws Exception {
        super(factory, configuration, false);
    }

    @Override
    protected IRepository createRepository(SystemFactory factory) {
        IRepository rep = null;
        try {
            rep = factory.createBlogRepository();
        } catch (PropertyVetoException ex) {
            log.severe(ex.getMessage());
        }
        return rep;
    }

    public static void main(String[] args) {
        RSSConf configuration = new RSSConf("blogcrawler_configuration.txt");
        SystemFactory systemFactory = new SystemFactory(configuration);
        try {
            BlogCrawler crawler = new BlogCrawler(systemFactory, configuration);
            crawler.startCrawling();
        } catch (Exception e) {
            log.severe(e.getMessage());
            System.exit(1);
        }
    }
}
