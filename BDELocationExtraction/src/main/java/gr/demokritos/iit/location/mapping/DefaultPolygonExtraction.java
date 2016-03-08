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
import gr.demokritos.iit.location.mapping.client.IRestClient;
import gr.demokritos.iit.location.mapping.client.JBossRestClient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DefaultPolygonExtraction implements IPolygonExtraction {

    private final String polURL;
    private final IRestClient client;

    private Gson gs;

    public DefaultPolygonExtraction(String locURL) {
        this.polURL = locURL;
        this.client = new JBossRestClient();
        this.gs = new Gson();
    }

    public DefaultPolygonExtraction(String locURL, IRestClient clientImpl) {
        this.polURL = locURL;
        this.client = clientImpl;
        this.gs = new Gson();
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
                res.put(locEnt, unwrapped.get(ind++).toJSON());
            }

        } catch (Exception ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    private List<GeocodeResponse> extractGeoCodes(String ent) {
        try {
            return gs.fromJson(ent, List.class);
        } catch (JsonSyntaxException ex) {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);
            // TODO: parse?
            return Collections.EMPTY_LIST;
        }
    }

    class GeocodeResponse {

        private final String type;
        private final GeoLoc[][] coordinates;

        public GeocodeResponse(String type, GeoLoc[][] coordinates) {
            this.type = type;
            this.coordinates = coordinates;
        }

        public String toJSON() {
            return gs.toJson(this);
        }
    }

    class GeoLoc {

        private double lng;
        private double lat;

        public GeoLoc(double lng, double lat) {
            this.lng = lng;
            this.lat = lat;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + (int) (Double.doubleToLongBits(this.lng) ^ (Double.doubleToLongBits(this.lng) >>> 32));
            hash = 29 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GeoLoc other = (GeoLoc) obj;
            if (Double.doubleToLongBits(this.lng) != Double.doubleToLongBits(other.lng)) {
                return false;
            }
            if (Double.doubleToLongBits(this.lat) != Double.doubleToLongBits(other.lat)) {
                return false;
            }
            return true;
        }
    }
}
