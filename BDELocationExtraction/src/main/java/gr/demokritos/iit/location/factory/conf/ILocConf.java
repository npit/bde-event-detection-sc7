/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.factory.conf;

import gr.demokritos.iit.base.conf.IBaseConf;

import java.util.ArrayList;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocConf extends IBaseConf {

    boolean onlyUpdateEvents();
    String getPolygonExtractionSourceFile();
    String getPolygonExtractionImpl();

    String getLocationExtractorConfig();
    String getEntityExtractorConfig();

    boolean shouldUpdateEvents();

    String getLocationExtractor();
    String getEntityExtractor();



    String getTokenProviderImpl();

    String getSentenceSplitterImpl();

    String getPolygonExtractionURL();

    String getSentenceSplitterModelPath();

    String getNEModelsDirPath();

    double getNEConfidenceCutOffThreshold();

    String getDocumentMode();

    String getRestClientImpl();

    String getDocumentRetrievalMode();

    String getDocumentListFile();

    String getExtractionObjective();

    boolean shouldExtractLocations(String mode);

    boolean shouldExtractEntities(String mode);

    String getMetadataProviderName();
    int getMaxResultsPerItem();
}
