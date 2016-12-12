package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.clustering.structs.SimilarityMode;

import java.util.Properties;

/**
 * Created by npittaras on 12/9/2016.
 */
public class clusteringConf extends BaseConfiguration implements IClusteringConf {
    public static final String ARTICLES="articles";
    public static final String TWEETS="both";
    public static final String BOTH="tweets";

    public clusteringConf(String path)
    {
        super(path);
    }
    public clusteringConf()
    {
        super();
    }


    @Override
    public OperationMode getOperationMode() {

        OperationMode def = IClusteringConf.OperationMode.PARALLEL;
        String value = properties.getProperty("operation_mode","");
        if(!value.isEmpty())
        {
            if(IClusteringConf.OperationMode.supports(value))
                def = OperationMode.valueOf(value.toUpperCase());
            else
                System.err.println(String.format("Operation mode [%s] unsupported. Using default: [%s]",value,def.toString()));

        }
        return def;

    }

    @Override
    public boolean sendToStrabon()
    {
	
	String value = properties.getProperty("send_to_strabon","no");
	if(value.equals("yes")) return true;
        return false;
    }
    @Override
    public boolean justSendToStrabon()
    {
	
	String value = properties.getProperty("only_send_to_strabon","no");
	if(value.equals("yes")) return true;
	return false;
    }
    @Override
    public String getStrabonURL()
    {
	return properties.getProperty("strabon_url");
    }
    @Override
    public double getTwitterMappingSimilarityThreshold()
    {
        double value = 0.063D;
        String inputThresh = properties.getProperty("twitter_sim_threshold","");
        if(!inputThresh.isEmpty())
            value = Double.parseDouble(inputThresh);
        return value;
    }
    @Override
    public double getTwitterMappingTitleSimilarityThreshold()
    {
        double value = 0.4D;
        String inputThresh = properties.getProperty("twitter_title_sim_threshold","");
        if(!inputThresh.isEmpty())
            value = Double.parseDouble(inputThresh);
        return value;
    }

    @Override
    public int getMaxNumberOfArticles()
    {
        int value = 500;
        String readValue = properties.getProperty("max_articles","");
        if(! readValue.isEmpty())
            value = Integer.parseInt(readValue);
        return value;
    }
    @Override
    public String getClusteringMode()
    {
        return  properties.getProperty("clustering_mode","base");
    }
    @Override
    public boolean shouldTriggerChangeDetection()
    {
        String value = properties.getProperty("trigger_change_detection","");
        return isTrue(value);
    }

    @Override
    public int getChangeDetectionThreshold()
    {
        int defaultThreshold = 5;
        int retVal = defaultThreshold;
        String value = properties.getProperty("change_detection_threshold","");

        try
        {
            retVal = Integer.parseInt(value);
        }
        catch(NumberFormatException ex)
        {
            System.out.println("Invalid value for [change_detection_threshold property] : [" + value + "]" );
            System.out.println("Using default value " + defaultThreshold);
        }

        return retVal;
    }

    @Override
    public String getChangeDetectionURL()
    {
        return properties.getProperty("change_detection_url","");
    }

    @Override
    public String getChangeDetectionUsername() {
        return properties.getProperty("change_detection_username","");
    }

    @Override
    public String getChangeDetectionPassword() {
        return properties.getProperty("change_detection_password","");
    }

    @Override
    public SimilarityMode getSimilarityMode() {
        try {
            SimilarityMode mode = SimilarityMode.valueOf(properties.getProperty(SIMILARITY_MODE_PARAM, "nvs").toUpperCase());
            return mode;
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println("fallback to default similarity mode: NVS");
            return SimilarityMode.NVS;
        }
    }

    @Override
    public double getCutOffThreshold() {
        return Double.valueOf(properties.getProperty(CUTOFF_THRESHOLD_PARAM, "0.24"));
    }

    @Override
    public int getEventSizeCutoffThreshold()
    {
        return Integer.parseInt(properties.getProperty("eventsize_cutoff_threshold","2"));
    }


    // Apache Spark


    @Override
    public String getMaster() {
        return properties.getProperty(SPARK_MASTER, "local[4]");
    }

    @Override
    public String getAppName() {
        return properties.getProperty(SPARK_APP_NAME, "EventDetection");
    }

    @Override
    public String getClusterExecutorMemory() {
        return properties.getProperty(SPARK_EXECUTOR_MEMORY, "1g");
    }

    @Override
    public int getNumDaysBatch() {
        return Integer.valueOf(properties.getProperty(BATCH_NUM_OF_DAYS, "2"));
    }

    @Override
    public int getNumPartitions() {
        return Integer.valueOf(properties.getProperty(NUM_PARTITIONS, "4"));
    }

    @Override
    public String getNGramMode()
    {
        return properties.getProperty("ngram_graph_mode", "word");
    }


}
