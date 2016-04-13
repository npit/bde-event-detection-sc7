package gr.demokritos.iit.clustering.repository;

import com.datastax.driver.core.Session;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Topic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gr.demokritos.iit.base.conf.IBaseConf.DATE_FORMAT_ISO_8601;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class DemoCassandraRepository extends BaseCassandraRepository {

    public DemoCassandraRepository(Session session) {
        super(session);
    }

    // use this class to save topics for DEMO
    public void saveEvents(Map<String, Summary> topics) {
        for (Topic t : topics.values()) {
            saveTopic(t);
        }
    }

    public void saveTopic(Topic topic) {
        String id = topic.getID();
        String title = topic.getTitle();
        String isoDateStr = Utils.toTimezoneFormattedStr(topic.getDate(), "UTC", DATE_FORMAT_ISO_8601);




    }


    public List<Article> loadArticlesAsDemo(long timestamp) {
        List<Article> res = new ArrayList();

        Collection<Map<String, Object>> items = loadArticles(timestamp);
        // wrap to Article instances
        for (Map<String, Object> eachItem : items) {
            String source_url = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
            String title = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName());
            String clean_text = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
            String feed_url = (String) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_FEED_URL.getColumnName());
            long published = (long) eachItem.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
            Date d = new Date();
            d.setTime(published);
            res.add(new Article(source_url, title, clean_text, "Europe", feed_url, null, d));
        }
        return res;
    }
}
