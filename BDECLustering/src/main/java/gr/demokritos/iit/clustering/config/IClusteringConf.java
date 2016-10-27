package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.IBaseConf;

/**
 * Created by npittaras on 12/9/2016.
 */
public interface IClusteringConf extends IBaseConf {
    boolean sendToStrabon();
    boolean justSendToStrabon();
    String getStrabonURL();
    double getTwitterMappingSimilarityThreshold();
    double getTwitterMappingTitleSimilarityThreshold();
    int getMaxNumberOfArticles();
    String getClusteringMode();

    boolean getTriggerChangeDetection();
    int getChangeDetectionThreshold();
}
