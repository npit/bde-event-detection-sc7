package gr.demokritos.iit.location;

import gr.demokritos.iit.location.factory.ILocFactory;
import gr.demokritos.iit.location.factory.LocationFactory;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import gr.demokritos.iit.location.mode.DocumentMode;
import gr.demokritos.iit.location.repository.ILocationRepository;

import java.util.logging.Level;

import static gr.demokritos.iit.location.factory.ILocFactory.LOG;

/**
 * Created by npittaras on 5/9/2016.
 */

// hotfix to produce articles & tweets PER DATE , if they are missing, from existing news_articles and twitter_post tables.
public class hotfix {

    public static void main(String[] args) {
        String path = "/home/npittaras/Documents/project/BDE/BDEproject/BDEEventDetection/BDELocationExtraction/res/hotfix_location_extraction.properties";

        ILocConf conf = new LocConf(path);
        ILocFactory factory = null;
        try {
            // get operation mode
            String mode = conf.getDocumentMode();
            DocumentMode operationMode = DocumentMode.valueOf(mode.toUpperCase());
            // instantiate a new factory
            factory = new LocationFactory(conf);
            // init connection pool to the repository
            ILocationRepository repos = factory.createLocationCassandraRepository();

            // do the hotfix
            // ------------------
            // create per published date tables
            //repos.createPerPublishedDateTables();
            // remove articles based on content
            //repos.removeUndesirableArticles();


        } catch (IllegalArgumentException  ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (factory != null) {
                // release connection with cluster
                factory.releaseResources();
            }
        }
    }
}
