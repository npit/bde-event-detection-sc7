package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.BaseConfiguration;

import java.util.Properties;

/**
 * Created by npittaras on 12/9/2016.
 */
public class clusteringConf extends BaseConfiguration implements IClusteringConf {
    Properties properties;
    public clusteringConf(String path)
    {
        super(path);
        this.properties = new Properties();
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


}
