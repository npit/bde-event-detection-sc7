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
package gr.demokritos.iit.location.mapping.client;

import com.google.gson.Gson;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class JBossRestClient implements IRestClient {

    @Override
    public String execGet(String base_url, Map<String, String> params, Class class_of_entity) throws Exception {
        ClientRequest req
                = get(base_url, params);
        return (String) req.get(class_of_entity).getEntity();
    }

    private ClientRequest get(String base_url, Map<String, String> params) {
        int i = 0;
        String url = "";
        if (params != null) {
            for (Map.Entry<String, String> entrySet : params.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                if (i++ == 0) {
                    url = url.concat("?").concat(key).concat("=").concat(value);
                } else {
                    url = url.concat("&").concat(key).concat("=").concat(value);
                }
            }
        }
        System.out.println(url);
        ClientRequestFactory crf = new ClientRequestFactory(UriBuilder.fromUri(base_url).build());
        return crf.createRelativeRequest(url);
    }

    @Override
    public String execJSONPost(String url, String json_data, Class response_entity_class) throws Exception {
        ClientRequestFactory crf = new ClientRequestFactory();
        ClientRequest req = crf.createRequest(url);
        req.accept(MediaType.APPLICATION_JSON);
        req.body(MediaType.APPLICATION_JSON, json_data);
        return (String) req.post(response_entity_class).getEntity();
    }

//    public static void main(String[] args) throws Exception {
//        IRestClient cl = new JBossRestClient();
//        List<String> places = new ArrayList();
//        places.add("Athens, Greece");
//        String json_data = new Gson().toJson(places, List.class);
//        ClientResponse execPost = (ClientResponse) cl.execJSONPost("http://popeye.di.uoa.gr:8080/changeDetection/location/geocode", json_data, String.class);
//        System.out.println(execPost.getEntity());
//    }
}
