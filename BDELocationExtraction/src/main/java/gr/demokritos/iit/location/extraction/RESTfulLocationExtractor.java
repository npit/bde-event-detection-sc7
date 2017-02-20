package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
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


    IRestfulFilter Filter;
    // property param names
    private final String API = "api";
    private final String ARG = "argument";
    private final String IN_FORMAT = "input_format";
    private final String OUT_FORMAT = "output_format";
    private final String OUT_SPEC = "output_filter";
    private final String NAME_VAL = "name_value_pairs";
    private final String AUTH = "auth";



    private ArrayList<String> urlParamNames, urlParamValues;    // param. names and values of the request
    String baseURL, authEncoded;    // base url of the request, encoded authentication info
    // you can run the extractor with the below main parameters for each article / tweet
    private final String[] SupportedArgModes = {"url", "text"};
    private final String[] SupportedOutputModes = {"json"};
    private final int JSON=0;


    private final LE_RESOURCE_TYPE[] SupportedModesRequire = {LE_RESOURCE_TYPE.URL, LE_RESOURCE_TYPE.CLEAN_TEXT};
    private String inputMode, outputMode;
    boolean Verbosity, Debug;
    public RESTfulLocationExtractor() {
        this.urlParamNames = new ArrayList<>();
        this.urlParamValues = new ArrayList<>();
        authEncoded="";
        Verbosity = false;
        Debug = false;

    }

    // return requested type
    @Override
    public LE_RESOURCE_TYPE getRequiredResource() {
        return SupportedModesRequire[Arrays.asList(SupportedArgModes).indexOf(inputMode)];
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
    // send the request
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
        if(Debug)
            System.out.println("\nAPI response:\n" + response + "\n");
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
                if(Verbosity)
                    System.out.println("\n\tBreaking up text due to too large URI.");
                response = splitTextAPICall(document);
            }
            System.out.println();
            if(Debug) {
                e.printStackTrace();
                System.err.flush();
            }
        }
        if(response == null)
            return Collections.EMPTY_SET;
        if(response.isEmpty())
            return Collections.EMPTY_SET;
        return parse(response);
    }

    @Override
    public Set<String> extractGenericEntities(String resource) {

        return Filter.getEntityType(IRestfulFilter.EntityType.GENERIC);
    }

    Set<String> parse(ArrayList<String> data)
    {
        if(outputMode.equals("json"))
        {
            Filter.filter(data);
            return removeAccents(Filter.getEntityType(IRestfulFilter.EntityType.LOCATION));
        }
        else
        {
            System.err.println("Restful LE reached parse phase with no valid output mode:" + outputMode);

            return null;
        }

    }
    private static Set<String> removeAccents(Set<String> setstr)
    {
        Set<String> res = new HashSet<>();
        for(String s : setstr)
        {
            res.add(removeAccents(s));
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

        String confFile = conf.getLocationExtractorConfig();
        if(confFile.isEmpty()) {
            System.err.println("No extractor config. file specified.");
            return false;
        }
        System.out.println("Reading RESTful LE configuration file: ["+ confFile+"]");

        Properties props = new Properties();
        try {
            props.load(new FileReader(new File(confFile)));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            Verbosity = conf.hasModifier(BaseConfiguration.Modifiers.VERBOSE.toString());
            Debug = conf.hasModifier(BaseConfiguration.Modifiers.DEBUG.toString());
            String str;
            // base api url
            // read property
            baseURL = props.getProperty(API, ""); if(baseURL.isEmpty()) throw new Exception("Empty " + API +  " parameter");

            str = props.getProperty(ARG,""); if(str.isEmpty()) throw new Exception("Empty " + ARG +  " parameter");
            // sanity check
            if(!Utils.arrayContains(SupportedArgModes,str))
            {
                System.err.println(SupportedArgModes);
                throw new Exception("Unsupported " + ARG + " parameter.");
            }
            urlParamNames.add(str); urlParamValues.add("");
            // out format
            str = props.getProperty(OUT_FORMAT,""); if(str.isEmpty()) throw new Exception("Empty " + OUT_FORMAT +  " parameter");
            if(!Utils.arrayContains(SupportedOutputModes,str))
            {
                System.err.println(SupportedOutputModes);
                throw new Exception("Unsupported " + OUT_FORMAT + " parameter.");
            }
            outputMode = str;
            // in format
            str = props.getProperty(IN_FORMAT,""); if(str.isEmpty()) throw new Exception("Empty " + IN_FORMAT +  " parameter");
            if(!Utils.arrayContains(SupportedArgModes,str))
            {
                System.err.println(SupportedArgModes);
                throw new Exception("Unsupported " + ARG + " parameter.");
            }
            inputMode = str;
            // out filter
            str = props.getProperty(OUT_SPEC,"");
            if( str.isEmpty())
            {
                System.out.println("Warning : No output specifier specified for RESTful LE. All results will be kept.");
            }
            // json out filter. Format : category:fieldToGet
            if(outputMode.equals(SupportedOutputModes[JSON])) {
                Filter = new RESTfulResultJSONFilter();
                if( ! Filter.initialize(str,this.Debug) ) throw new Exception("Failed to initialize REST LE JSON filter.");
            }
            // name-value
            str = props.getProperty(NAME_VAL, "");
            if (!str.isEmpty()) {
                // name - value

                String[] parts = str.split(",");
                for (String part : parts) {
                    String[] nameval = part.split("=");
                    if (nameval.length != 2) throw new Exception("Error @ reading name-value: " + part);
                    urlParamNames.add(nameval[0].trim());
                    urlParamValues.add(nameval[1].trim());
                }
            }

            // auth
            str = props.getProperty(AUTH,"");
            if(str.isEmpty())
            {
                System.out.println("Warning : No authentication specified for RESTful LE.");
                return true;
            }
            Base64 enc = new Base64();
            authEncoded =  enc.encodeToString( (str).getBytes() );
            authEncoded = authEncoded.replaceAll("\n","");
            tellStatus();

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
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
        System.out.println("filter" + ":[" + Filter +"]");
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
