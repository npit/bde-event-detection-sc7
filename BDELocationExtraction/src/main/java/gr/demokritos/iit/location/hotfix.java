package gr.demokritos.iit.location;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.factory.ILocFactory;
import gr.demokritos.iit.location.factory.LocationFactory;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.location.schedule.ILocationExtractionScheduler;
import gr.demokritos.iit.location.schedule.LocationExtractionScheduler;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import static gr.demokritos.iit.location.factory.ILocFactory.LOG;
import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

/**
 * Created by npittaras on 5/9/2016.
 */

// hotfix to produce articles & tweets PER DATE , if they are missing, from existing news_articles and twitter_post tables.
public class hotfix {

    public static void main(String[] args) {
        String path = "/home/npittaras/Documents/project/BDE/BDEproject/BDEEventDetection/BDELocationExtraction/res/location_extraction.properties";

        ILocConf conf = new LocConf(path);
        ILocFactory factory = null;
        try {
            // get operation mode
            String mode = conf.getOperationMode();
            OperationMode operationMode = OperationMode.valueOf(mode.toUpperCase());
            // instantiate a new factory
            factory = new LocationFactory(conf);
            // init connection pool to the repository
            ILocationRepository repos = factory.createLocationCassandraRepository();

            // create per published date tables
            repos.doHotfix();


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
