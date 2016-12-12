package gr.demokritos.iit.location.mapping.client;

import gr.demokritos.iit.base.util.Utils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Created by nik on 12/5/16.
 */
public class SimpleRestClient implements IRestClient {
    @Override
    public String execGet(String base_url, Map<String, String> params, Class class_of_entity) throws Exception {
        int count = 0;
        String targetURL=base_url;
        for(String paramName : params.keySet())
        {
            if (count == 0) base_url+="?";
            else base_url+="&";
            targetURL += java.net.URLEncoder.encode(params.get(paramName),"UTF-8");
        }
        String response = Utils.sendGET(targetURL);
        return  response;
    }

    @Override
    public String execJSONPost(String base_url, String json_data, Class class_of_entity) throws Exception {

        String response = Utils.sendPOST(json_data,base_url);
        return response;

    }
}
