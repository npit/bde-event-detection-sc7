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
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by nik on 12/29/16.
 */
public class PoolPartyLocationExtractor implements ILocationExtractor {
    private ArrayList<String> urlParamNames, urlParamValues;
    String baseURL, authEncoded;
    // you can run the extractor with the below main parameters for each article
    private final String[] SupportedModes = {"url", "text"};
    private final LE_RESOURCE_TYPE[] SupportedModesRequire = {LE_RESOURCE_TYPE.URL, LE_RESOURCE_TYPE.CLEAN_TEXT};

    public PoolPartyLocationExtractor() {
        this.urlParamNames = new ArrayList<>();
        this.urlParamValues = new ArrayList<>();
        authEncoded="";

    }

    @Override
    public LE_RESOURCE_TYPE getRequiredResource() {
        return SupportedModesRequire[Arrays.asList(SupportedModes).indexOf(urlParamNames.get(0))];
    }

    private ArrayList<String> splitTextAPICall(String document)
    {
        ArrayList<String> res = new ArrayList<>();
        int numChunks = 2;
        while(true)
        {
            //System.out.println("\tSpliting to " + numChunks + " parts.");
            // split text
            ArrayList<String> parts = new ArrayList<>();

            String []chunks = document.split(" ");
            int chunksPerPart = chunks.length / numChunks;

            int counter = 0, partIndex=0;
            String accumulatedStr = "";
            for(String chunk : chunks)
            {

                if(counter++ > chunksPerPart)
                {
                    // add accumulated string to the list
                    parts.add(accumulatedStr);
                    accumulatedStr = "";
                    counter = 0;
                }
                accumulatedStr += chunk + " ";
            }
            if(!accumulatedStr.isEmpty())
                parts.add(accumulatedStr);
            // try the GET
            String response;
            try {
                for (String part : parts) {
                    String payload = formPayload(part);
                    String partialResponse = contactAPI(baseURL + payload, authEncoded);
                    if(partialResponse == null) continue;
                    if(partialResponse.isEmpty()) continue;
                    res.add(partialResponse);
                }
                return res;
            } catch (IOException e) {
                if(e.getMessage().contains("HTTP response code: 414"))
                {
                    ++numChunks;
                    continue;
                }
            }

            return res;
        }
    }

    private String contactAPI(String url, String auth) throws IOException
    {
        String response = null;

//        System.out.println("CONTACT API, url:");
//        System.out.println(url);
//        System.out.println();
        response = Utils.sendGETAuth(url, authEncoded);
        return response;

    }

    private String formPayload(String articleArgument)
    {
        urlParamValues.set(0,articleArgument);
        return Utils.encodeParameterizedURL(urlParamNames,urlParamValues);
    }
    @Override
    public Set<String> extractLocation(String document) {
        if (document == null || document.trim().isEmpty()) {
            return Collections.EMPTY_SET;
        }

        // set article url
        String payload = formPayload(document);

        // use an arraylist, since we may have to split the article text, in that mode.
        ArrayList<String> response = new ArrayList<>();

        try {
            String apiResponse = contactAPI(baseURL + payload, authEncoded);
            response.add(apiResponse);
        } catch (IOException e) {
            if(e.getMessage().contains("HTTP response code: 414"))
            {
                //System.out.println("\n\tBreaking up text due to too large URI.");
                response = splitTextAPICall(document);
            }
        }
        if(response == null)
            return null;
        if(response.isEmpty())
            return null;
        return parse(response);
    }

    Set<String> parse(ArrayList<String> data)
    {
        Set<String> res = new HashSet<>();
        JSONParser parser = new JSONParser();

        for(String datum : data) {
            try {

                JSONObject obj = (JSONObject) parser.parse(new StringReader(datum));

                JSONArray locations = (JSONArray) obj.get("locations");
                if (locations != null) {
                    for (Object o : locations) {
                        String rawName = (String) ((JSONObject) o).get("name");
                        res.add(removeAccents(rawName));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
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
            System.err.println("Poolparty config needs at least 2 params: the base API URL, and the param to assign the article url.");
            System.err.println("Instead found params:{" + contents + "}");
            return false;
        }
        baseURL = contents.get(0);
        // article-url parameter or article-text is the first after the base api url
        String articleParameter = contents.get(1);
        if(!Arrays.asList(SupportedModes).contains(articleParameter))
        {
            System.err.println("Unsupported article parameter : " + articleParameter + "]. Supported parameter modes are:");
            System.err.println(SupportedModes);
            return false;
        }
        urlParamNames.add(articleParameter);

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
        tellStatus();
        return true;
    }

    private void tellStatus()
    {
        System.out.println("Poolparty config:");
        System.out.println("----------------------------");
        System.out.println("baseurl:[" + baseURL +"]");
        for(int i=0;i< urlParamNames.size(); ++i)
        {
            System.out.println(urlParamNames.get(i) + ":[" + urlParamValues.get(i) +"]");
        }
        System.out.println("----------------------------");
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
