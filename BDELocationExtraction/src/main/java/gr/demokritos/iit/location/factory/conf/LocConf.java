/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.factory.conf;

import gr.demokritos.iit.base.conf.BaseConfiguration;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocConf extends BaseConfiguration implements ILocConf {

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
    public String getOperationMode() {
        return properties.getProperty("operation_mode");
    }

    @Override
    public String getLocationExtractionImpl() {
        return properties.getProperty("location_extraction_impl");
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
    public String getLocationNameDatasetFile(){ return properties.getProperty("location_extraction_dataset");}
    @Override
    public boolean useAdditionalExternalNames(){
        String value = properties.getProperty("use_additional_external_location_src","");
        if(!value.isEmpty())
        {
            if(value.toLowerCase().equals("yes")) return true;
        }
        return false;
    }
}
