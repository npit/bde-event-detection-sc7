/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.schedule;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.factory.LocationFactory;
import gr.demokritos.iit.location.extraction.BaseLocationExtractor;
import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.extraction.provider.EnhancedOpenNLPTokenProvider;
import gr.demokritos.iit.location.extraction.provider.ITokenProvider;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.mapping.DefaultPolygonExtraction;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.structs.LocSched;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocationExtractor {

    public static void main(String[] args) throws IOException {
        IBaseConf conf = new BaseConfiguration("../BDETwitterListener/res/twitter.properties");
        LocationFactory factory = null;
        try {

            factory = new LocationFactory(conf);

            // init connection pool to the repository
            ILocationRepository repos = factory.createLocationCassandraRepository();

            // register starting operation
            LocSched sched = repos.scheduleInitialized();
            System.out.println("last parsed: " + new Date(sched.getLastParsed()).toString());
            // init location extractor
            ITokenProvider tp = new EnhancedOpenNLPTokenProvider(); // use default paths for debug TODO FIXME
            ILocationExtractor locExtractor = new BaseLocationExtractor(tp);

            // load articles to process from last_parsed indicator.
            Collection<Map<String, Object>> loadArticles = repos.loadArticles(sched.getLastParsed());

            // load polygon extraction client
            IPolygonExtraction poly = new DefaultPolygonExtraction(); // TODO: add parameters

            // keep most recent published for reference
            long max_published = Long.MIN_VALUE;
            int i = 0;
            // for each article
            for (Map<String, Object> article : loadArticles) {
                long published = (long) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName());
                max_published = Math.max(max_published, published);

                String permalink = (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName());
                String clean_text = (String) article.get(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName());
                Set<String> locationsFound = locExtractor.extractLocation(clean_text);
                if (!locationsFound.isEmpty()) {
                    Map<String, String> places_polygons = extractPolygons(locationsFound, poly);
                    // update entry
                    repos.updateArticleWithPlaceMetadata(permalink, places_polygons);
                    i++;
                }
            }
            sched.setItemsUpdated(i);
            // update last timestamp parsed
            sched.setLastParsed(max_published);
            System.out.println("last parsed: " + new Date(max_published).toString());
            // register completed
            repos.scheduleFinalized(sched);
        } finally {
            if (factory != null) {
                // release connection with cluster
                factory.releaseResources();
            }
        }
    }

    private static Map<String, String> extractPolygons(Set<String> locationsFound, IPolygonExtraction poly) {
        Map<String, String> res = new HashMap();
        for (String loc : locationsFound) {
            // TODO handle nulls.
            res.put(loc, poly.extractPolygon(loc));
        }
        return res;
    }
}
