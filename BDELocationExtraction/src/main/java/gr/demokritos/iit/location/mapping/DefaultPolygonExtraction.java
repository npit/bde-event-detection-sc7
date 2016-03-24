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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import gr.demokritos.iit.location.mapping.client.IRestClient;
import gr.demokritos.iit.location.mapping.client.JBossRestClient;

import javax.ws.rs.core.Response;
import java.util.*;
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

    public DefaultPolygonExtraction(String locURL) {
        this.polURL = locURL;
        this.client = new JBossRestClient();
        // for debugging purposes
//        this.client = new DebugRestClient();
    }

    public DefaultPolygonExtraction(String locURL, IRestClient clientImpl) {
        this.polURL = locURL;
        this.client = clientImpl;
    }

    @Override
    public String extractPolygon(String locationEntity) {
        throw new UnsupportedOperationException("not supported (yet?)");
    }

    @Override
    public Map<String, String> extractPolygon(Collection<String> locationEntities) {
        // TODO: implement API call to get polygon
        // TODO: test!
        Map<String, String> res = new HashMap();
        try {
            Response response = client.execJSONPost(polURL, gs.toJson(locationEntities, Collection.class), String.class);
            String ent = (String) response.getEntity();

            List<GeocodeResponse> unwrapped = extractGeoCodes(ent);

            // coords are supposed to arrive in the same index.
            int ind = 0;
            for (String locEnt : locationEntities) {
                GeocodeResponse gr = unwrapped.get(ind++);
                if (gr != null) {
                    res.put(locEnt, gr.toJSON());
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    private List<GeocodeResponse> extractGeoCodes(String ent) {
        // TODO : test with real DATA
        try {
            TypeToken type_token = new TypeToken<List<GeocodeResponse>>() {};
            return gs.fromJson(ent, type_token.getType());
        } catch (JsonSyntaxException ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
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
