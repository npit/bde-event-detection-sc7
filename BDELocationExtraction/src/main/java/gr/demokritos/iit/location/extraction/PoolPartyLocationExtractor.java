package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by nik on 12/29/16.
 */
public class PoolPartyLocationExtractor implements ILocationExtractor {
    private ArrayList<String> urlParamNames, urlParamValues;
    String baseURL, authEncoded;

    public PoolPartyLocationExtractor() {
        this.urlParamNames = new ArrayList<>();
        this.urlParamValues = new ArrayList<>();
        authEncoded="";

    }

    @Override
    public LE_RESOURCE_TYPE getRequiredResource() {
        return LE_RESOURCE_TYPE.URL;
    }

    @Override
    public Set<String> extractLocation(String document) {
        if (document == null || document.trim().isEmpty()) {
            return Collections.EMPTY_SET;
        }

        // set article url
        urlParamValues.set(0,document);
        String payload = Utils.encodeParameterizedURL(urlParamNames,urlParamValues);
        String response = null;
        try {
            response = Utils.sendGETAuth(baseURL + payload, authEncoded);
        } catch (IOException e) {
            e.printStackTrace();

        }
        if(response == null)
            return null;
        if(response.isEmpty())
            return null;
        return parse(response);
    }

    Set<String> parse(String data)
    {
        Set<String> res = new HashSet<>();

        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new StringReader(data));

            JSONArray locations = (JSONArray) obj.get("locations");
            if(locations != null) {
                for (Object o : locations) {
                    String rawName =(String) ((JSONObject) o).get("name");
                    res.add(removeAccents(rawName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return res;
    }

    private static String removeAccents(String str) {
        String res;
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        res = pattern.matcher(nfdNormalizedString).replaceAll("");
        res = res.replaceAll("[^A-Za-z0-9 ]"," ");
        return res;
    }
    @Override
    public boolean configure(ILocConf conf) {
        // For pool party LE, use the location extraction source file for config
        // It should contain
        //
        // // API call example:
        //
//        https://bde.poolparty.biz/extractor/api/extract?
//      url=http://feeds.reuters.com/~r/Reuters/worldNews/~3/9X3IBEcr6ug/us-israel-palestinians-un-idUSKBN14B033&language=en&projectId=1DE00F04-C1B9-0001-404F-52256800BE20&locationExtraction=true
//
//
//        1DE00F04-C1B9-0001-404F-52256800BE20


        String ppConfFile = conf.getLocationExtractionSourceFile();
        System.out.println("Reading poolparty LE configuration file: ["+ ppConfFile+"]");

        ArrayList<String> contents = Utils.readFileLinesDropComments(ppConfFile);
        if(contents == null)
        {
            System.err.println("Poolparty config file parse error.");
            return false;
        }
        if(contents.size() < 2)
        {
            System.err.println("Poolparty config needs at least 2 params: the base URL, and the param to assign the article url.");
            System.err.println("Instead found params:{" + contents + "}");
            return false;
        }
        baseURL = contents.get(0);
        // article-url parameter is the first after the base api url
        urlParamNames.add(contents.get(1));
        urlParamValues.add("");
        if(contents.size() <= 3) return true; // no params specfied

        String delimiter = contents.get(2);
        for(String p : contents.subList(3,contents.size()))
        {

            String [] nameval = p.split(delimiter);
            if(nameval[0].equals("authentication"))
            {
                Base64 enc = new Base64();
                authEncoded = nameval[1];
                authEncoded =  enc.encodeToString( (authEncoded).getBytes() );
                authEncoded = authEncoded.replaceAll("\n","");
                continue;
            }
            urlParamNames.add(nameval[0].trim());
            urlParamValues.add(nameval[1].trim());
        }
        if(authEncoded.isEmpty())
        {
            System.err.println("No username, password fields were provided in configuration file [" + ppConfFile + "].");
            return false;
        }
        return true;
    }


    public static void main(String [] args)
    {
        PoolPartyLocationExtractor pp = new PoolPartyLocationExtractor();
        ILocConf conf = new LocConf("/home/nik/work/iit/BDE/bde-event-detection-sc7/BDELocationExtraction/res/location.properties");
        pp.configure(conf);
        String test = "Republic of Côte d'Angelo";
        System.out.println(removeAccents(test));
    }
}
