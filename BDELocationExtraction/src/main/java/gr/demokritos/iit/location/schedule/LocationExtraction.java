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

import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.factory.ILocFactory;
import gr.demokritos.iit.location.factory.LocationFactory;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.mode.OperationMode;
import gr.demokritos.iit.location.repository.ILocationRepository;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import static gr.demokritos.iit.location.factory.ILocFactory.LOG;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocationExtraction {

    public static void main(String[] args) throws IOException {


        String path = "./res/location_extraction.properties";
        if (args.length == 0) {
            System.out.println(USAGE);
            System.out.println(String.format("Using default path for configuration file: %s%n", path));
        } else {
            System.out.println(String.format("Provided configuration file path: [%s]\n",args[0]));
            path = args[0];
        }
        ILocConf conf = new LocConf(path);
        ILocFactory factory = null;
        try {
            // get operation mode
            String mode = conf.getDocumentMode();
            OperationMode documentMode = OperationMode.valueOf(mode.toUpperCase());
            // instantiate a new factory
            factory = new LocationFactory(conf);
            // init connection pool to the repository
            ILocationRepository repos = factory.createLocationCassandraRepository();


            if(conf.onlyUpdateEvents())
            {
                repos.onlyUpdateEventsWithExistingLocationInformation();
            }
            else
            {
                if(conf.shouldUpdateEvents())
                    repos.setUpdateEvents();
                // init location extractor

                ILocationExtractor locExtractor = factory.createLocationExtractor();
                if(locExtractor == null) return;

                if(locExtractor.configure(conf) == false) {
                    System.out.println("Location extractor configuration failed.");
                    factory.releaseResources();
                    return;
                }

                // load polygon extraction client
                IPolygonExtraction poly = factory.createPolygonExtractionClient();
                // according to mode, execute location extraction schedule.
                ILocationExtractionScheduler scheduler = new LocationExtractionScheduler(
                        documentMode, repos, locExtractor, poly, conf
                );
                String retrieval_mode = conf.getDocumentRetrievalMode();
                if(retrieval_mode.equals(LocConf.RETRIEVAL_MODE_SCHEDULED))
                    scheduler.executeSchedule();
                else if(retrieval_mode.equals(LocConf.RETRIEVAL_MODE_EXPLICIT))
                    scheduler.executeTargetedUpdate();
                else
                {
                    System.err.println("Undefined document retrieval mode : [" + retrieval_mode + "]");
                    System.err.println("Available modes are: " + LocConf.modes());
                    return;
                }
            }
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        finally
        {
            if (factory != null) {
                // release connection with cluster
                factory.releaseResources();
            }
        }
    }

    private static final String USAGE = String.format("%nexample usage: java -cp $CP %s /path/to/properties_file"
            + "%nIf no arguments provided, will use the properties file in ./res/ catalog.%n", LocationExtraction.class.getName());
}
