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
package gr.demokritos.iit.location.mapping;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import gr.demokritos.iit.location.mapping.client.DebugRestClient;
import gr.demokritos.iit.location.mapping.client.IRestClient;
import gr.demokritos.iit.location.mapping.client.JBossRestClient;
import gr.demokritos.iit.location.mapping.client.SimpleRestClient;
import gr.demokritos.iit.location.util.GeometryFormatTransformer;
import org.json.simple.parser.ParseException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DefaultPolygonExtraction implements IPolygonExtraction {

    private final String polURL;
    private final IRestClient client;

    private final Gson gs = new Gson();

    private Set<String> polyWarnCache;
    public Set<String> getFailedExtractionNames() {
        return polyWarnCache;
    }

    @Override
    public void init() {
        polyWarnCache = new HashSet<>();

    }

    /**
     * cache location items for an hour
     */
    private static final long MAX_CACHE_DURATION_MINUTES = 60l;

    // each call to the API lasts ~1.5 seconds, so use a simple caching mechanism to avoid redundant calls
    private final Cache<String, String> location_cache;

    public DefaultPolygonExtraction(String locURL) {
	System.out.println("Initializing remote polygon extractor, @ url : " + locURL +  ".");
        this.polURL = locURL;
        this.client = new SimpleRestClient();
        this.location_cache  = CacheBuilder.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000l)
                .expireAfterWrite(MAX_CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    public DefaultPolygonExtraction(String locURL, long max_cache_size, IRestClient clientImpl) {
        this.polURL = locURL;
        this.client = clientImpl;
        this.location_cache  = CacheBuilder.newBuilder()
                .initialCapacity(1)
                .maximumSize(max_cache_size)
                .expireAfterWrite(MAX_CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public String extractPolygon(final String locationEntity) {
        if (locationEntity == null || locationEntity.trim().isEmpty()) {
            return "";
        }
        // ask cache
        String geoloc = location_cache.getIfPresent(locationEntity);

        if (geoloc != null) {
            return geoloc;
        }
        String res = "";
        // API accepts only JsonArray
        final Collection<String> input = new ArrayList() {{add(locationEntity);}};
        try {
            String responseEntity = client.execJSONPost(polURL, gs.toJson(input, Collection.class), String.class);

            // debug!
            //System.out.println("POST response entity : " + ent); //debugprint
            // debug!
            // responses from the API: when smth wrong: 'null', when error in call (?) 'code:400, message:exception"
            if (responseEntity != null && !responseEntity.contains("null") && !responseEntity.equals("{\"code\":400,\"message\":\"exception\"}")) {
                // add to cache
                location_cache.put(locationEntity, responseEntity);
                res = responseEntity;
            }
            // when the server returns html junk
            if(responseEntity.contains("The requested resource is not available."))
            {
                throw new Exception ("Polygon extraction server says resource is not available.");
            }
        } catch (Exception ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        return res;
    }

    @Override
    public Map<String, String> extractPolygon(Collection<String> locationEntities) {
        Map<String, String> res = new HashMap();

        for (String loc : locationEntities) {
            String poly = extractPolygon(loc);
            if (poly != null && !poly.isEmpty()) {
                res.put(loc, poly);
            }
            else
            {
                if( ! polyWarnCache.contains(loc)) {
                    polyWarnCache.add(loc);
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Polygon extraction failed for [" + loc + "]");
                }
            }
        }
        return res;
    }
    // we do not use this to avoid extra costs; instead we save as a string and let whoever who uses it just parse the string
    private List<GeocodeResponse> extractGeoCodes(String ent) {
//        if (ent.contains("exception")) {
//            return Collections.EMPTY_LIST;
//        }
        try {
            TypeToken type_token = new TypeToken<List<GeocodeResponse>>() {};
            return gs.fromJson(ent, type_token.getType());
        } catch (JsonSyntaxException ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
            System.out.print("ENTITY: ");
            System.out.println(ent);
            // TODO: we somehow have to parse the JSON manually to ignore the exception cases.
            // OR we should ask for alike responses, i.e. code:200, message:"", data:{}
            return Collections.EMPTY_LIST;
        }
    }

    class GeocodeResponse {

        private final String type;
        private final Object coordinates;

        public GeocodeResponse(String type, Object coordinates) {
            this.type = type;
            this.coordinates = coordinates;
        }

        public String toJSON() {
            return new Gson().toJson(this, GeocodeResponse.class);
        }
    }


    public Map<String,String> postProcessGeometries(Map<String,String> places_polygons)
    {
        Map<String,String> out = new HashMap();
        for(String location : places_polygons.keySet())
        {
            String value = places_polygons.get(location);
            ArrayList<String> wktvalues = new ArrayList();
            try {
                wktvalues = GeometryFormatTransformer.GeoJSONtoWKTList(value);
            }
            catch(org.json.simple.parser.ParseException ex)
            {
                ex.printStackTrace();
                continue;
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
                continue;
            }
            if(wktvalues.size() > 1)
            {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Encountered multiple-geometries location: [" + location + "]");
                for(String geom : wktvalues)
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Geometry: " + geom + "");
            }
            out.put(location,wktvalues.get(0));

        }
        return out;
    }

    public static void main(String [] args)
    {

        DefaultPolygonExtraction dpe = new DefaultPolygonExtraction("http://teleios4.di.uoa.gr:8080/changeDetection/location/geocode");
        String poly = dpe.extractPolygon("Barcelona");
        try {
            poly = GeometryFormatTransformer.GeoJSONtoWKT(poly);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        System.out.println(poly);

    }

}
