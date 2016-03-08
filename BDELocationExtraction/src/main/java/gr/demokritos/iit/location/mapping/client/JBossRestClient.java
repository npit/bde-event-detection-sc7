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

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class JBossRestClient implements IRestClient {

    @Override
    public Response execGet(String base_url, Map<String, String> params, Class class_of_entity) throws Exception {
        ClientRequest req
                = get(base_url, params);
        return (Response) req.get(class_of_entity);
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
        ClientRequest req = crf.createRelativeRequest(url);
        return req;
    }

    @Override
    public Response execJSONPost(String url, String json_data, Class response_entity_class) throws Exception {
        ClientRequestFactory crf = new ClientRequestFactory();
        ClientRequest req = crf.createRequest(url);
        req.accept(MediaType.APPLICATION_JSON);
        req.body(MediaType.APPLICATION_JSON, json_data);
        ClientResponse<String> post = req.post(response_entity_class);
        return post;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> params = new HashMap();
//        params.put("lang", null)
        IRestClient cl = new JBossRestClient();
//        ClientResponse execGet = (ClientResponse) cl.execGet("http://143.233.226.97:60091/InfoAssetService/categories/getCategories", null, String.class);
        ClientResponse execPost = (ClientResponse) cl.execJSONPost("http://83.212.114.19:8084/InfoAssetService/sources/insert", "{\n"
                + "\"source_type\":\"RSS\",\n"
                + "\"source_url\":\"http://www.test/feed=rss\",\n"
                + "\"source_label\": \"source_label_1\",\n"
                + "\"source_category\":\"category_1\",\n"
                + "\"refresh_time\": 30,\n"
                + "\"source_lang\": \"en\"\n"
                + "}", String.class);
        System.out.println(execPost.getEntity());
    }
}
