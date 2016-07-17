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
import gr.demokritos.iit.location.util.GeometryFormatTransformer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
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
            path = args[0];
        }
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
            // init location extractor
            ILocationExtractor locExtractor = factory.createDefaultLocationExtractor();
            // load polygon extraction client
            IPolygonExtraction poly = factory.createDefaultPolygonExtractionClient();
            // according to mode, execute location extraction schedule.
            ILocationExtractionScheduler scheduler = new LocationExtractionScheduler(
                operationMode, repos, locExtractor, poly
            );
            scheduler.executeSchedule();
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (factory != null) {
                // release connection with cluster
                factory.releaseResources();
            }
        }
    }

    private static final String USAGE = String.format("%nexample usage: java -cp $CP %s /path/to/properties_file"
            + "%nIf no arguments provided, will use the properties file in ./res/ catalog.%n", LocationExtraction.class.getName());
}
