
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.factory.conf;

import gr.demokritos.iit.base.conf.BaseConfiguration;

import java.util.ArrayList;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocConf extends BaseConfiguration implements ILocConf {

    public static final String RETRIEVAL_MODE_SCHEDULED="scheduled";
    public static final String RETRIEVAL_MODE_EXPLICIT="explicit";
    public static String modes()
    {
        return RETRIEVAL_MODE_SCHEDULED + "," + RETRIEVAL_MODE_EXPLICIT;
    }
    public LocConf(String configurationFileName) {
        super(configurationFileName);
    }

    public LocConf() {
        super();
    }

    @Override
    public String getPolygonExtractionURL() {
        return properties.getProperty("polygon_extraction_url");
    }

    @Override
    public String getTokenProviderImpl() {
        return properties.getProperty("token_provider_impl");
    }

    @Override
    public String getSentenceSplitterImpl() {
        return properties.getProperty("sentence_splitter_impl");
    }

    @Override
    public String getSentenceSplitterModelPath() {
        return properties.getProperty("sentence_splitter_model");
    }

    @Override
    public String getNEModelsDirPath() {
        return properties.getProperty("ne_models_path");
    }

    @Override
    public double getNEConfidenceCutOffThreshold() {
        return Double.parseDouble(properties.getProperty("ne_confidence_cut_off"));
    }

    @Override
    public String getDocumentMode() {
        return properties.getProperty("document_mode");
    }

    @Override
    public String getRestClientImpl() {
        return properties.getProperty("rest_client_impl","");
    }

    @Override
    public String getDocumentListFile() {
        return properties.getProperty("document_list_file","");
    }

    @Override
    public String getLocationExtractor() {
        String res = properties.getProperty("extractor","");
        if(res.isEmpty()) {
            System.err.println("Location extractor not specified, using default.");
            res = "default";
        }
        return res;
    }


    @Override
    public String getPolygonExtractionImpl()
    {
        return properties.getProperty("polygon_extraction_impl");
    }
    @Override
    public String getPolygonExtractionSourceFile()
    {
        return properties.getProperty("polygon_extraction_sourcefile");
    }

    @Override
    public String getLocationExtractorConfig(){ return properties.getProperty("extractor_config","");}

    @Override
    public boolean onlyUpdateEvents()
    {
        String value = properties.getProperty("only_update_events","");
        if(!value.isEmpty())
        {
            if(value.toLowerCase().equals("yes")) return true;
        }
        return false;
    }
    @Override
    public boolean shouldUpdateEvents()
    {
        String value = properties.getProperty("should_update_events","");
        if(!value.isEmpty())
        {
            if(value.toLowerCase().equals("yes")) return true;
        }
        return false;
    }
    @Override
    public String getDocumentRetrievalMode()
    {
        return properties.getProperty("document_retrieval_mode",RETRIEVAL_MODE_SCHEDULED);
    }

    @Override
    public String getExtractionObjective()
    {
        return properties.getProperty("extraction_objective","all");
    }


}
