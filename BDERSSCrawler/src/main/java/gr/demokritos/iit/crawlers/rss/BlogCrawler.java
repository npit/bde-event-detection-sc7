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

import gr.demokritos.iit.crawlers.rss.factory.conf.IRSSConf;
import gr.demokritos.iit.crawlers.rss.factory.RSSCrawlFactory;
import gr.demokritos.iit.crawlers.rss.factory.conf.RSSConf;
import static gr.demokritos.iit.crawlers.rss.factory.RSSCrawlFactory.log;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.beans.PropertyVetoException;

public class BlogCrawler extends AbstractCrawler {

    public BlogCrawler(RSSCrawlFactory factory, IRSSConf configuration) throws Exception {
        super(factory, configuration, false);
    }

    @Override
    protected IRepository createRepository(RSSCrawlFactory factory) {
        IRepository rep = null;
        try {
            rep = factory.createBlogRepository();
        } catch (PropertyVetoException ex) {
            log.severe(ex.getMessage());
        }
        return rep;
    }

    public static void main(String[] args) {
        IRSSConf configuration=null;
        String configFile = "";

        if(args.length > 0) configFile=args[0];
        else configFile= DEFAULT_BLOG_CONFIGURATION;
        configuration = new RSSConf(configFile);
        RSSCrawlFactory factory = null;
        try {
            factory = new RSSCrawlFactory(configuration);
            BlogCrawler crawler = new BlogCrawler(factory, configuration);
            crawler.startCrawling();
        } catch (Exception e) {
            log.severe(e.getMessage());
            System.exit(1);
        } finally {
            if (factory != null) {
                factory.releaseResources(); // CAUTION only on run_forever = false
            }
        }
    }
}
