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

import javax.ws.rs.core.Response;
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

    /**
     * cache location items for an hour
     */
    private static final long MAX_CACHE_DURATION_MINUTES = 60l;

    // each call to the API lasts ~1.5 seconds, so use a simple caching mechanism to avoid redundant calls
    private final Cache<String, String> location_cache;

    public DefaultPolygonExtraction(String locURL) {
        this.polURL = locURL;
        this.client = new JBossRestClient();
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
        System.out.print("Location extraction POST for " + locationEntity + "... ");
        // ask cache
        String geoloc = location_cache.getIfPresent(locationEntity);

        if (geoloc != null) {
            System.out.println("Cache hit!");
            return geoloc;
        }
        String res = "";
        // API accepts only JsonArray
        final Collection<String> input = new ArrayList() {{add(locationEntity);}};
        System.out.println("Miss!\n\tLocation extraction POST ...");
        long startTime = System.currentTimeMillis(); //debugprint , timing
        try {
            Response response = client.execJSONPost(polURL, gs.toJson(input, Collection.class), String.class);
            String ent = (String) response.getEntity();
            // debug!
            //System.out.println(ent); //debugprint
            // debug!
            // responses from the API: when smth wrong: 'null', when error in call (?) 'code:400, message:exception"
            if (ent != null && !ent.contains("null") && !ent.equals("{\"code\":400,\"message\":\"exception\"}")) {
                // add to cache
                location_cache.put(locationEntity, ent);
                res = ent;
            }
        } catch (Exception ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }

        long duration = (System.currentTimeMillis() - startTime);
        System.out.println("\ttime took " + Long.toString(duration) + " msec");

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
}
