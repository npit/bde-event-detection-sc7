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
package gr.demokritos.iit.location.schedule;

import gr.demokritos.iit.base.repository.views.Cassandra;
import gr.demokritos.iit.location.factory.ILocFactory;
import static gr.demokritos.iit.location.factory.ILocFactory.LOG;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import gr.demokritos.iit.location.factory.LocationFactory;
import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.location.structs.LocSched;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocationExtractionSchedule {

    public static void main(String[] args) throws IOException {
        String path = "./res/location_extraction.properties";
        if (args.length == 0) {
            System.out.println(USAGE);
            System.out.println(String.format("Using default path for configuration file: %s%n", path));
        } else {
            path = args[0];
        }
        ILocConf conf = new LocConf(path);
        ILocFactory factory = null;
        try {

            factory = new LocationFactory(conf);

            // init connection pool to the repository
            ILocationRepository repos = factory.createLocationCassandraRepository();

            // register starting operation
            LocSched sched = repos.scheduleInitialized();
            System.out.println("last parsed: " + new Date(sched.getLastParsed()).toString());
            // init location extractor
            ILocationExtractor locExtractor = factory.createDefaultLocationExtractor();

            // load articles to process from last_parsed indicator.
            Collection<Map<String, Object>> loadArticles = repos.loadArticles(sched.getLastParsed());

            // load polygon extraction client
            IPolygonExtraction poly = factory.createDefaultPolygonExtractionClient();

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
//                    Map<String, String> places_polygons = extractPolygons(locationsFound, poly);
                    Map<String, String> places_polygons = poly.extractPolygon(locationsFound);
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
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (factory != null) {
                // release connection with cluster
                factory.releaseResources();
            }
        }
    }

    /**
     * @deprecated 
     * @param locationsFound
     * @param poly
     * @return 
     */
    private static Map<String, String> extractPolygons(Set<String> locationsFound, IPolygonExtraction poly) {
        Map<String, String> res = new HashMap();
        for (String loc : locationsFound) {
            String polygon = poly.extractPolygon(loc);
            if (polygon != null && !polygon.trim().isEmpty()) {
                res.put(loc, polygon);
            }
        }
        return res;
    }

    private static final String USAGE = String.format("%nexample usage: java -cp $CP %s /path/to/properties_file"
            + "%nIf no arguments provided, will use the properties file in ./res/ catalog.%n", LocationExtractionSchedule.class.getName());
}
