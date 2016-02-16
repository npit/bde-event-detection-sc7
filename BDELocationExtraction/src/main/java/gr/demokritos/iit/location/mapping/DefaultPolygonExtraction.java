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

import gr.demokritos.iit.location.mapping.client.IRestClient;
import gr.demokritos.iit.location.mapping.client.JBossRestClient;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DefaultPolygonExtraction implements IPolygonExtraction {

    private final String polURL;
    private final IRestClient client;

    public DefaultPolygonExtraction(String locURL) {
        this.polURL = locURL;
        this.client = new JBossRestClient();
    }

    public DefaultPolygonExtraction(String locURL, IRestClient clientImpl) {
        this.polURL = locURL;
        this.client = clientImpl;
    }

    @Override
    public String extractPolygon(String locationEntity) {
        // TODO: implement API call to get polygon
        // DEBUG
        return "POLYGON("
                + "("
                + "35.312207138944146 25.300386450309578,"
                + "35.312207138944146 19.257905982399958,"
                + "41.09114708620481 19.257905982399958,"
                + "41.09114708620481 25.300386450309578,"
                + "35.312207138944146 25.300386450309578"
                + ")"
                + ")";
    }
}
