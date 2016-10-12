/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.factory.conf;

import gr.demokritos.iit.base.conf.IBaseConf;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocConf extends IBaseConf {

    boolean onlyUpdateEvents();
    String getPolygonExtractionSourceFile();
    String getPolygonExtractionImpl();
    String getLocationExtractionSourceFile();
    boolean useAdditionalExternalNames();

    String getLocationExtractionImpl();

    String getTokenProviderImpl();

    String getSentenceSplitterImpl();

    String getPolygonExtractionURL();

    String getSentenceSplitterModelPath();

    String getNEModelsDirPath();

    double getNEConfidenceCutOffThreshold();

    String getOperationMode();



}
