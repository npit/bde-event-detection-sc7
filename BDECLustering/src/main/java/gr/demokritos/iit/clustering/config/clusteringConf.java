package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.BaseConfiguration;

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
        String value = properties.getProperty("mode",BOTH);
        if(value.equals(BOTH));
        else if (value.equals(ARTICLES));
        else if(value.equals(BOTH));
        else
        {
            System.err.println("Undefined supplied clustering mode: [" + value + "]");
            System.err.println("Using default [both]");
            return BOTH;
        }
        return value;

    }

}
