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

// implements location extraction by querying an online RESTful service
public class RESTfulLocationExtractor implements ILocationExtractor {
    private ArrayList<String> urlParamNames, urlParamValues;    // param. names and values of the request
    String baseURL, authEncoded;    // base url of the request, encoded authentication info
    // you can run the extractor with the below main parameters for each article / tweet
    private final String[] SupportedModes = {"url", "text"};
    private final String[] SupportedOutputModes = {"json"};
    private final LE_RESOURCE_TYPE[] SupportedModesRequire = {LE_RESOURCE_TYPE.URL, LE_RESOURCE_TYPE.CLEAN_TEXT};
    private String outputMode, outputSPecifier;
    public RESTfulLocationExtractor() {
        this.urlParamNames = new ArrayList<>();
        this.urlParamValues = new ArrayList<>();
        authEncoded="";

    }

    @Override
    public LE_RESOURCE_TYPE getRequiredResource() {
        return SupportedModesRequire[Arrays.asList(SupportedModes).indexOf(urlParamNames.get(0))];
    }

    // if the text is too large to fit in a URI, split it as many times as needed and perform separate calls
    // split is done around a white space
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
        if(authEncoded.isEmpty())
            response = Utils.sendGET(url);
        else
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
            return Collections.EMPTY_SET;
        if(response.isEmpty())
            return Collections.EMPTY_SET;
        return parse(response);
    }

    Set<String> parseJSON(ArrayList<String> data)
    {
        Set<String> res = new HashSet<>();
        JSONParser parser = new JSONParser();

        for(String datum : data) {
            try {

                JSONObject obj = (JSONObject) parser.parse(new StringReader(datum));

                JSONArray locations = (JSONArray) obj.get(outputSPecifier);
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

    Set<String> parse(ArrayList<String> data)
    {
        if(outputMode.equals("json"))
            return parseJSON(data);
        else
        {
            System.err.println("Restful LE reached parse phase with no valid output mode!");
            return null;
        }

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

        String confFile = conf.getLocationExtractorConfig();
        if(confFile.isEmpty()) {
            System.err.println("No extractor config. file specified.");
            return false;
        }
        System.out.println("Reading RESTful LE configuration file: ["+ confFile+"]");

        ArrayList<String> contents = Utils.readFileLinesDropComments(confFile);
        int contentIdx=0;

        if(contents == null)
        {
            System.err.println("RESTful LE config file parse error.");
            return false;
        }
        if(contents.size() < 4)
        {
            System.err.println("RESTful LE config needs at least 4 params:");
            System.err.println("the base API URL,\n the param to assign the article url/text to\nthe output format,\nthe output specifier.");
            System.err.println("Instead found params:{" + contents + "}");
            return false;
        }
        baseURL = contents.get(contentIdx++);
        // article-url parameter or article-text is the first after the base api url
        String articleParameter = contents.get(contentIdx++);
        if(!Arrays.asList(SupportedModes).contains(articleParameter))
        {
            System.err.println("Unsupported article parameter : " + articleParameter + "]. Supported parameter modes are:");
            System.err.println(SupportedModes);
            return false;
        }
        urlParamNames.add(articleParameter);

        outputMode = contents.get(contentIdx++);
        if(!Arrays.asList(SupportedOutputModes).contains(outputMode))
        {
            System.err.println("Unsupported article parameter : " + outputMode + "]. Supported parameter modes are:");
            System.err.println(SupportedOutputModes);
            return false;
        }

        outputSPecifier = contents.get(contentIdx++);

        urlParamValues.add("");
        if(contents.size() == contentIdx) return true; // no params specfied

        String delimiter = contents.get(contentIdx++);
        for(String p : contents.subList(contentIdx,contents.size()))
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
//      Not mandatory in all restful LEs, isn't it now?
//        if(authEncoded.isEmpty())
//        {
//            System.err.println("No username, password fields were provided in configuration file [" + confFile + "].");
//            return false;
//        }
        tellStatus();
        return true;
    }

    private void tellStatus()
    {
        System.out.println("RESTful LE config:");
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
        RESTfulLocationExtractor pp = new RESTfulLocationExtractor();
        ILocConf conf = new LocConf("/home/nik/work/iit/BDE/bde-event-detection-sc7/BDELocationExtraction/res/location.properties");
        pp.configure(conf);
        String test = "Republic of Côte d'Angelo";
        System.out.println(removeAccents(test));
    }
}
