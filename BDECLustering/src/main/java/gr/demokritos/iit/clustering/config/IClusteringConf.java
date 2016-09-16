package gr.demokritos.iit.clustering.config;

import gr.demokritos.iit.base.conf.IBaseConf;

/**
 * Created by npittaras on 12/9/2016.
 */
public interface IClusteringConf extends IBaseConf {
    public boolean sendToStrabon();
    public boolean justSendToStrabon();
    public String getStrabonURL();
}
