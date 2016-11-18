package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.clustering.structs.SimilarityMode;

/**
 * Created by npittaras on 12/9/2016.
 */
public interface IClusteringConf extends IBaseConf {

    String SIMILARITY_MODE_PARAM = "similarity_mode";
    String CUTOFF_THRESHOLD_PARAM = "cutoff_threshold";

    boolean sendToStrabon();

    boolean justSendToStrabon();

    String getStrabonURL();

    double getTwitterMappingSimilarityThreshold();

    double getTwitterMappingTitleSimilarityThreshold();

    int getMaxNumberOfArticles();

    String getClusteringMode();

    boolean shouldTriggerChangeDetection();

    int getChangeDetectionThreshold();

    String getChangeDetectionURL();

    String getChangeDetectionUsername();

    String getChangeDetectionPassword();

    double getCutOffThreshold();

    SimilarityMode getSimilarityMode();


}
