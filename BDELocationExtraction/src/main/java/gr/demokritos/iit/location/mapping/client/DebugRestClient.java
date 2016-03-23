package gr.demokritos.iit.location.mapping.client;

import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.BaseClientResponse;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 * @date 3/23/16
 */
public class DebugRestClient implements IRestClient {
    @Override
    public Response execGet(String base_url, Map<String, String> params, Class class_of_entity) throws Exception {
        return null;
    }

    @Override
    public Response execJSONPost(String base_url, String json_data, Class class_of_entity) throws Exception {
        String single_example = "{\"type\":\"Polygon\",\"coordinates\":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]}";
        Response.ResponseBuilder b = Response.ok().entity(Arrays.toString(new String[] {single_example, single_example}));
        return b.build();
    }
}
