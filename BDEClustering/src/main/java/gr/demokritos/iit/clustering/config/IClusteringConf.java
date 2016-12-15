package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.clustering.structs.SimilarityMode;


/**
 * Created by npittaras on 12/9/2016.
 */
public interface IClusteringConf extends IBaseConf {

    enum OperationMode {

        PARALLEL("parallel"), DISTRIBUTED("distributed");
        private String type;

        private OperationMode(String type) {
            this.type = type;
        }
        public static boolean supports(String candidate)
        {
            for(OperationMode elem : OperationMode.values())
            {
                if(elem.toString().equals(candidate)) return true;
            }
            return false;
        }
        @Override
        public String toString() {
            return type;
        }
    }
    String SIMILARITY_MODE_PARAM = "similarity_mode";
    String CUTOFF_THRESHOLD_PARAM = "cutoff_threshold";

    OperationMode getOperationMode();

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

    int getEventSizeCutoffThreshold();

    SimilarityMode getSimilarityMode();

    double getCutOffThreshold();


    // Apache spark configuration

    String SPARK_EXECUTOR_MEMORY = "spark_memory";
    String SPARK_CASSANDRA_CONNECTION_HOST = "spark.cassandra.connection.host";
    String SPARK_CASSANDRA_CONNECTION_PORT = "spark.cassandra.connection.port";
    String SPARK_MASTER = "spark_master";
    String SPARK_APP_NAME = "spark_app_name";
    String BATCH_NUM_OF_DAYS = "batch_days_limit";
    String NUM_PARTITIONS = "num_partitions";


    String getMaster();

    String getAppName();

    String getClusterExecutorMemory();

    /**
     *
     * @return days back to load articles batch from now
     */
    int getNumDaysBatch();

    /**
     * in how many partitions shall we operate upon
     * @return
     */
    int getNumPartitions();

    String getNGramMode();


}
