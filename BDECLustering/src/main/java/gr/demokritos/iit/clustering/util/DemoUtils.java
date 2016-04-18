package gr.demokritos.iit.clustering.util;

import au.com.bytecode.opencsv.CSVWriter;
import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;
import org.apache.log4j.Category;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.asset.social.classification.IClassifier;
import org.scify.asset.social.clustering.SocialMediaClusterer;
import org.scify.asset.social.data.preprocessing.DefaultSocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.ISocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.IStemmer;
import org.scify.asset.social.data.preprocessing.TwitterStemmer;
import org.scify.newsum.server.clustering.BaseArticleClusterer;
import org.scify.newsum.server.clustering.IArticleClusterer;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.nlp.sentsplit.DefaultSentenceSplitter;
import org.scify.newsum.server.nlp.sentsplit.ISentenceSplitter;
import org.scify.newsum.server.summarization.ISummarizer;
import org.scify.newsum.server.summarization.Summarizer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class DemoUtils {


    private static final Logger LOGGER = Logger.getLogger(DemoUtils.class.getName());

    public static void main(String[] args) {

        String properties = "./res/clustering.properties";
        if (args.length == 1) {
            properties = args[0];
        }
        IBaseConf conf = new BaseConfiguration(properties);
        DemoClusteringFactory factory = null;
        DemoCassandraRepository repository;
        try {
            factory = new DemoClusteringFactory(conf);
            repository = factory.createDemoCassandraRepository();

            LOGGER.info("loading events...");

            Collection<Map<String, Object>> events = repository.loadEvents(1000);

            exportToCSV(events, "/home/gkioumis/Documents/Demokritos/BDE/sample_events.csv");

            LOGGER.info("Done");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (factory != null) {
                factory.releaseResources();
            }
        }
    }

    public static void exportToCSV(Collection<Map<String, Object>> items, String pathToCSV) throws IOException {
        CSVWriter csvwriter = new CSVWriter(new FileWriter(pathToCSV));
        int i = 0;
        for (Map<String, Object> item : items) {
            if (++i == 1) {
                // write header
                String[] header = new String[item.keySet().size()];
                header[0] = "event_id";
                header[1] = "title";
                header[2] = "description";
                header[3] = "event_date";
                header[4] = "place_mappings";
                header[5] = "event_source_urls";
                header[6] = "tweet_ids";
                csvwriter.writeNext(header);
            }
            String id = (String) item.get(Cassandra.Event.TBL_EVENTS.FLD_EVENT_ID.getColumnName());
            String title = (String) item.get(Cassandra.Event.TBL_EVENTS.FLD_TITLE.getColumnName());
            String description = (String) item.get(Cassandra.Event.TBL_EVENTS.FLD_DESCRIPTION.getColumnName());
            String event_date = (String) item.get(Cassandra.Event.TBL_EVENTS.FLD_DATE_LITERAL.getColumnName());
            String place_mappings = ((Map<String, String>) item.get(Cassandra.Event.TBL_EVENTS.FLD_PLACE_MAPPINGS.getColumnName())).toString();
            String source_urls = ((Set<String>) item.get(Cassandra.Event.TBL_EVENTS.FLD_EVENT_SOURCE_URLS.getColumnName())).toString();
            String tweetIDs = ((Set<Long>) item.get(Cassandra.Event.TBL_EVENTS.FLD_TWEET_IDS.getColumnName())).toString();
            // append to writer
            csvwriter.writeNext(new String[]{id, title, description, event_date, place_mappings, source_urls, tweetIDs});
        }

        csvwriter.flush();
        csvwriter.close();
    }

}
