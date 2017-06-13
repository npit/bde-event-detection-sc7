package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.factory.conf.LocConf;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by nik on 12/29/16.
 */

// implements location extraction by querying an online RESTful service
public class RESTfulLocationExtractor extends  BaseLocationExtractor implements ILocationExtractor {


    IRestfulFilter Filter;
    // property param names
    private final String API = "api";
    private final String ARG = "argument";
    private final String IN_FORMAT = "input_format";
    private final String OUT_FORMAT = "output_format";
    private final String OUT_SPEC = "output_filter";
    private final String NAME_VAL = "name_value_pairs";
    private final String AUTH = "auth";

    boolean DoExtractLocations, DoExtractEntities;

    private ArrayList<String> urlParamNames, urlParamValues;    // param. names and values of the request
    String baseURL, authEncoded;    // base url of the request, encoded authentication info
    // you can run the extractor with the below main parameters for each article / tweet

    private final String[] SupportedOutputModes = {"json"};
    private final int JSON=0;


    private final LE_RESOURCE_TYPE[] SupportedArgModes = LE_RESOURCE_TYPE.values();
    private final String[] SupportedArgModesStr = new String[SupportedArgModes.length];
    private String inputMode, outputMode;
    boolean Verbosity, Debug;
    private ArrayList<String >paramArguments;
    public RESTfulLocationExtractor(String extractorObjective) {
        this.urlParamNames = new ArrayList<>();
        this.urlParamValues = new ArrayList<>();
        paramArguments = new ArrayList<>();
        authEncoded="";
        Verbosity = false;
        Debug = false;

        DoExtractLocations=false;
        DoExtractEntities=false;

        if(extractorObjective.equals("locations")) DoExtractLocations=true;
        if(extractorObjective.equals("entities")) DoExtractEntities=true;
        this.objective = extractorObjective;
    }


    // if the text is too large to fit in a URI, split it as many times as needed and perform separate calls
    // split is done around a white space
    private ArrayList<String> splitTextAPICall(String document)
    {
        ArrayList<String> res = new ArrayList<>();
        int numChunks = 2;
        while(true)
        {
            if(Verbosity)
                System.out.println("\tSpliting to " + numChunks + " parts.");
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
                try {
                    if (e.getMessage().contains("HTTP response code: 414")) {
                        ++numChunks;
                        continue;
                    }
                    else
                    {
                        System.err.println(e.getMessage());
                        throw new Exception("Error response from rest api");
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.getMessage());
                    res =  null;
                }
            }


            return res;
        }
    }
    // send the request
    private String contactAPI(String url, String auth) throws IOException
    {
        String response = null;

        if(Verbosity)
            System.out.println("url: " + url);
        if(authEncoded.isEmpty())
            response = Utils.sendGET(url);
        else
            response = Utils.sendGETAuth(url, authEncoded);
        if(Debug)
            System.out.println("\nAPI response:\n" + response + "\n");
        return response;

    }

    private String formPayload(List<String> args)
    {

        if(paramArguments.size() != args.size())
        {
            // an arg was not assigned!
            System.err.println(String.format("ERROR : payload expects %d arguments : %s , provide %d : %s\n",paramArguments.size(),paramArguments.toString(), args.size(), args.toString()));
            return "";
        }
        int argidx = 0;
        for(String param : paramArguments)
        {
            int idx = urlParamNames.indexOf(param);
            urlParamValues.set(idx,args.get(argidx++));
        }
        return Utils.encodeParameterizedURL(urlParamNames,urlParamValues);
    }
    private String formPayload(String arg)
    {
        urlParamValues.set(0,arg);
        return Utils.encodeParameterizedURL(urlParamNames,urlParamValues);
    }
    @Override
    public List<String> doExtraction(Object dataObject) {
        this.Filter.clear();
        List<String> res;


        String payload = "";
        // set arguments
        if(paramArguments.size() > 1)
        {
            List<String> dataarr = (List<String>) dataObject;
            payload = formPayload(dataarr);
        }
        else
        {
            String datastr = (String) dataObject;
            if (datastr == null || datastr.trim().isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            payload = formPayload(datastr);
        }


        // use an arraylist, since we may have to split the article text, in that mode.
        ArrayList<String> response = new ArrayList<>();

        try {
            String apiResponse = contactAPI(baseURL + payload, authEncoded);
            if(Verbosity)
                System.out.println("\nApi call:\n\t" + baseURL + payload);
            response.add(apiResponse);
        } catch (IOException e) {
            if(e.getMessage().contains("HTTP response code: 414"))
            {
                if(paramArguments.size() == 1) {
                    if (Verbosity)
                        System.out.println("\n\tBreaking up text due to too large URI.");
                    String datastr = (String) dataObject;
                    response = splitTextAPICall(datastr);
                }
                else
                {
                    System.err.println("Cannot handle request fragmentation in multi-argument calls!");
                    res =  Collections.EMPTY_LIST;
                }
            }
            System.out.println();
            if(Debug) {
                e.printStackTrace();
                System.err.flush();
            }
        }
        if(response == null)
            res =  Collections.EMPTY_LIST;
        else if(response.isEmpty())
            res = Collections.EMPTY_LIST;
        else
            res = parse(response);

        return res;
    }

//    @Override
//    public Set<String> extractGenericEntities(String resource) {
//        if(!DoExtractEntities) return new HashSet<>();
//        return Filter.getEntityType(IRestfulFilter.EntityType.GENERIC);
//    }

    List<String> parse(ArrayList<String> data)
    {
        if(outputMode.equals("json"))
        {
            Filter.filter(data);
            List<String> res = Filter.getEntities();
            if(DoExtractLocations) res = removeAccents(res);
            return res;
        }
        else
        {
            System.err.println("Restful LE reached parse phase with no valid output mode:" + outputMode);

            return null;
        }

    }
    private static List<String> removeAccents(List<String> setstr)
    {
        List<String> res = new ArrayList<>();
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

        for (int i=0;i<SupportedArgModes.length;++i)SupportedArgModesStr[i] = SupportedArgModes[i].toString();

        String confFile = "";
        if(this.DoExtractEntities) confFile =  conf.getEntityExtractorConfig();
        if(this.DoExtractLocations) confFile =  conf.getLocationExtractorConfig();
        if(this.DoExtractLocations && this.DoExtractEntities)
        {
            System.err.println("Restful extractor was set to multiple roles.");
            return false;
        }

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

            str = props.getProperty(ARG,""); if(str.isEmpty()) throw new Exception("Empty " + ARG +  " parameter(s)");
            // multiple-parameterizable argument
            String [] parts = str.split(",");
            for (String part : parts )
            {
                urlParamNames.add(part); urlParamValues.add(""); // empty values denote pluginnable fields
                paramArguments.add(part);
            }

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
            if(!Utils.arrayContains(SupportedArgModesStr,str))
            {
                System.err.println(SupportedArgModesStr);
                throw new Exception("Unsupported " + ARG + " parameter " + str + ".");
            }
            RequiredResource = LE_RESOURCE_TYPE.valueOf(str.toUpperCase());
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
                if( ! Filter.initialize(str,DoExtractEntities,this.Debug) ) throw new Exception("Failed to initialize REST LE JSON filter.");
            }
            // name-value
            str = props.getProperty(NAME_VAL, "");
            if (!str.isEmpty()) {
                // name - value

                parts = str.split(",");
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

//        this.RequiredResource = SupportedArgModes[Arrays.asList(SupportedArgModes).indexOf(inputMode)];

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
        RESTfulLocationExtractor pp = new RESTfulLocationExtractor("entities");
        ILocConf conf = new LocConf("/home/nik/work/iit/BDE/bde-event-detection-sc7/BDELocationExtraction/res/location.properties");
        pp.configure(conf);
        String test = "Republic of Côte d'Angelo";
        System.out.println(removeAccents(test));
    }
}
