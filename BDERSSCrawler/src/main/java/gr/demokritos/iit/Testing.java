/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit;

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

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class Testing {

    public static void main(String[] args) throws PropertyVetoException, IOException, BoilerpipeProcessingException {
        IRSSConf conf = new RSSConf("res/newscrawler_configuration.properties");
        SystemFactory factory = new SystemFactory(conf);
        IRepository createNewsRepository = factory.createNewsRepository();
//        CrawlId findMostRecentCrawlId = createNewsRepository.findMostRecentCrawlId();
//        if (findMostRecentCrawlId == null) {
//            findMostRecentCrawlId = new CrawlId(1);
//        } else if (!findMostRecentCrawlId.hasFinishedCrawling()) {
//            findMostRecentCrawlId.finishedCrawling();
//        }
//        createNewsRepository.saveCrawlId(new CrawlId(2));
//        CrawlId findMostRecentCrawlId = createNewsRepository.findMostRecentCrawlId();
//        System.out.println(findMostRecentCrawlId.toString());

        Item item = new Item("feed_url_1", new CrawlId(1));
        Content content = new Content("content_url_1", "<html>raw text</html>", "etag", "after some time", new Date());
//                createNewsRepository.updateFeedMetaData(content);
        Date published = new Date();
        createNewsRepository.savePage(item, content, published);

        factory.releaseResources();
    }
}
