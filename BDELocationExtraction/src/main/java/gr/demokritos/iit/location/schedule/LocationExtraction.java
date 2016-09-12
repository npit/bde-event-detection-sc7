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

import com.vividsolutions.jts.io.ParseException;
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

        String gj="[{\"type\":\"Polygon\",\"coordinates\":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]},{\"type\":\"Polygon\",\"coordinates\":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]}]";
        String wkt="MULTIPOLYGON (((8.13125038146978 40.8618049621583, 8.17569065094 40.8618049621583, 8.17569065094 40.9723625183105, 8.20874977111828 40.9723625183105, 8.20874977111828 41.1209716796876, 8.45872211456293 41.1209716796876, 8.45872211456293 40.8618049621583, 8.46539497375488 40.8618049621583, 8.46539497375488 40.8794746398926, 8.63125133514416 40.8794746398926, 8.63125133514416 40.9190292358398, 8.77347183227539 40.9190292358398, 8.77347183227539 40.9498596191407, 8.82901477813726 40.9498596191407, 8.82901477813726 40.9881935119629, 8.86152839660645 40.9881935119629, 8.86152839660645 41.1073608398438, 8.98854732513433 41.1073608398438, 8.98854732513433 41.1736793518068, 8.99791622161871 41.1736793518068, 8.99791622161871 41.1780014038087, 9.137360572815 41.1780014038087, 9.137360572815 41.2593040466309, 9.31538677215582 41.2593040466309, 9.31538677215582 41.207359313965, 9.32097244262707 41.207359313965, 9.32097244262707 41.3123626708984, 9.48930931091309 41.3123626708984, 9.48930931091309 41.1736793518068, 9.77902793884277 41.1736793518068, 9.77902793884277 40.7868690490724, 9.72735977172857 40.7868690490724, 9.72735977172857 40.7527656555177, 9.75291919708252 40.7527656555177, 9.75291919708252 40.6800651550294, 9.75347042083735 40.6800651550294, 9.75347042083735 40.6156997680665, 9.82847023010265 40.6156997680665, 9.82847023010265 40.4731292724609, 9.80902862548828 40.4731292724609, 9.80902862548828 40.3319473266602, 9.6787509918214 40.3319473266602, 9.6787509918214 40.2251586914062, 9.73652935028082 40.2251586914062, 9.73652935028082 39.9865455627443, 9.70680522918713 39.9865455627443, 9.70680522918713 39.9583053588867, 9.71652793884283 39.9583053588867, 9.71652793884283 39.8673324584961, 9.69707965850836 39.8673324584961, 9.69707965850836 39.7961273193361, 9.67680454254162 39.7961273193361, 9.67680454254162 39.7430610656738, 9.67735862731928 39.7430610656738, 9.67735862731928 39.6267395019532, 9.65791988372797 39.6267395019532, 9.65791988372797 39.5506248474121, 9.65180683135986 39.5506248474121, 9.65180683135986 39.41463470459, 9.63458442687988 39.41463470459, 9.63458442687988 39.2412490844728, 9.60930824279797 39.2412490844728, 9.60930824279797 39.1331939697266, 9.56875133514399 39.1331939697266, 9.56875133514399 39.080696105957, 9.44235038757336 39.080696105957, 9.44235038757336 39.1212501525879, 9.18447589874262 39.1212501525879, 9.18447589874262 39.1611976623536, 9.18314743041992 39.1611976623536, 9.18314743041992 39.15625, 9.07624435424816 39.15625, 9.07624435424816 39.1002464294433, 9.04597187042248 39.1002464294433, 9.04597187042248 39.0212516784669, 9.02708244323742 39.0212516784669, 9.02708244323742 38.9165954589844, 8.91291713714594 38.9165954589844, 8.91291713714594 38.8756904602051, 8.84356689453125 38.8756904602051, 8.84356689453125 38.8643074035644, 8.60597324371344 38.8643074035644, 8.60597324371344 38.9551391601564, 8.5645837783814 38.9551391601564, 8.5645837783814 39.0208206176758, 8.55003261566168 39.0208206176758, 8.55003261566168 39.044303894043, 8.51927661895758 39.044303894043, 8.51927661895758 38.8590278625488, 8.37197780609125 38.8590278625488, 8.37197780609125 39.0106887817384, 8.3493061065675 39.0106887817384, 8.3493061065675 39.1134719848634, 8.42625045776379 39.1134719848634, 8.42625045776379 39.1086463928222, 8.43313694000244 39.1086463928222, 8.43313694000244 39.1559715270996, 8.35457992553711 39.1559715270996, 8.35457992553711 39.2496604919434, 8.37263774871838 39.2496604919434, 8.37263774871838 39.380012512207, 8.37831401824957 39.380012512207, 8.37831401824957 39.4492034912109, 8.37902832031261 39.4492034912109, 8.37902832031261 39.7704200744629, 8.50625038146978 39.7704200744629, 8.50625038146978 39.8317642211915, 8.5368061065675 39.8317642211915, 8.5368061065675 39.8576393127443, 8.29458427429199 39.8576393127443, 8.29458427429199 39.9993057250977, 8.37625026702881 39.9993057250977, 8.37625026702881 40.0625495910645, 8.47069072723383 40.0625495910645, 8.47069072723383 40.0638961791993, 8.45569229125977 40.0638961791993, 8.45569229125977 40.2153015136719, 8.45680618286133 40.2153015136719, 8.45680618286133 40.2587127685547, 8.47708415985113 40.2587127685547, 8.47708415985113 40.2627716064454, 8.37847232818615 40.2627716064454, 8.37847232818615 40.3956604003906, 8.36874198913574 40.3956604003906, 8.36874198913574 40.4945831298829, 8.13735961914074 40.4945831298829, 8.13735961914074 40.6528015136719, 8.13125038146978 40.6528015136719, 8.13125038146978 40.8618049621583)), ((8.21875000000006 39.1965293884278, 8.32569026947021 39.1965293884278, 8.32569026947021 39.0929183959961, 8.21875000000006 39.0929183959961, 8.21875000000006 39.1965293884278))) ";

        try {
            System.out.println("GeoToWKT:  " + gj + "\n\t" + GeometryFormatTransformer.GeoJSONtoWKT(gj));
            System.out.println("WKTtoGEO:  " + wkt + "\n\t" + GeometryFormatTransformer.WKTtoGeoJSON(wkt));
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        catch(ParseException ex)
        {
            ex.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
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
            String mode = conf.getOperationMode();
            OperationMode operationMode = OperationMode.valueOf(mode.toUpperCase());
            // instantiate a new factory
            factory = new LocationFactory(conf);
            // init connection pool to the repository
            ILocationRepository repos = factory.createLocationCassandraRepository();
            // init location extractor
            ILocationExtractor locExtractor = factory.createDefaultLocationExtractor();
            // load polygon extraction client
            IPolygonExtraction poly = factory.createPolygonExtractionClient();
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
