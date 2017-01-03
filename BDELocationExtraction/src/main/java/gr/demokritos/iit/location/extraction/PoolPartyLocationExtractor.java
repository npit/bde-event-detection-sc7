package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.factory.conf.ILocConf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Created by nik on 12/29/16.
 */
public class PoolPartyLocationExtractor implements ILocationExtractor {
    private ArrayList<String> urlParamNames, urlParamValues;
    String baseURL, articleParam;

    public PoolPartyLocationExtractor() {
        this.urlParamNames = new ArrayList<>();
        this.urlParamValues = new ArrayList<>();

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


        return null;
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
        ArrayList<String> contents = Utils.readFileLines(ppConfFile);
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
        // article url parameter is the first
        urlParamNames.add(contents.get(1));
        urlParamValues.add("");
        if(contents.size() <= 3) return true; // no params specfied

        String delimiter = contents.get(3);
        for(String p : contents.subList(2,contents.size()))
        {

            String [] nameval = p.split(delimiter);
            urlParamNames.add(nameval[0].trim());
            urlParamValues.add(nameval[1].trim());
        }
        return true;
    }
}
