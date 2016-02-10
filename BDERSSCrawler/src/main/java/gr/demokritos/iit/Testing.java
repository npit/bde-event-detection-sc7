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
package gr.demokritos.iit;

import com.datastax.driver.core.utils.UUIDs;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import gr.demokritos.iit.factory.IRSSConf;
import gr.demokritos.iit.factory.RSSConf;
import gr.demokritos.iit.factory.SystemFactory;
import gr.demokritos.iit.model.Content;
import gr.demokritos.iit.model.CrawlId;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.repository.IRepository;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class Testing {

    public static void main(String[] args) throws PropertyVetoException, IOException, BoilerpipeProcessingException {
        IRSSConf conf = new RSSConf("res/newscrawler_configuration.properties");
        SystemFactory factory = new SystemFactory(conf);
//        IRepository createNewsRepository = factory.createNewsRepository();
//        CrawlId findMostRecentCrawlId = createNewsRepository.findMostRecentCrawlId();
//        if (findMostRecentCrawlId == null) {
//            findMostRecentCrawlId = new CrawlId(1);
//        } else if (!findMostRecentCrawlId.hasFinishedCrawling()) {
//            findMostRecentCrawlId.finishedCrawling();
//        }
//        createNewsRepository.saveCrawlId(new CrawlId(2));
//        CrawlId findMostRecentCrawlId = createNewsRepository.findMostRecentCrawlId();
//        System.out.println(findMostRecentCrawlId.toString());

//        Item item = new Item("feed_url_1", new CrawlId(1));
//        Content content = new Content("content_url_1", "<html>raw text</html>", "etag", "after some time", new Date());
////                createNewsRepository.updateFeedMetaData(content);
//        Date published = new Date();
//        createNewsRepository.savePage(item, content, published);
//
//        factory.releaseResources();
//        long timestamp = new Date().getTime();
//        System.out.println(timestamp);
//        UUID endOf = UUIDs.endOf(timestamp);
//        System.out.println(endOf.toString());
//        
//        System.out.println(UUIDs.unixTimestamp(endOf));
        
        
    }
}
