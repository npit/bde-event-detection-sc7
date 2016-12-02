package gr.demokritos.iit.clustering.factory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import com.sun.corba.se.spi.orb.Operation;
import gr.demokritos.iit.clustering.config.IClusteringConf;
import gr.demokritos.iit.clustering.repository.ClusteringCassandraSparkRepository;
import gr.demokritos.iit.clustering.repository.ClusteringCassandraRepository;
import gr.demokritos.iit.clustering.repository.IClusteringRepository;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.asset.social.classification.IClassifier;
import org.scify.asset.social.classification.SocialMediaClassifier;
import org.scify.asset.social.clustering.SocialMediaClusterer;
import org.scify.asset.social.data.preprocessing.IStemmer;
import org.scify.newsum.server.clustering.IArticleClusterer;
import org.scify.newsum.server.model.structures.Topic;

import java.util.Collection;
import java.util.Map;

/**
 * Created by npittaras on 2/12/2016.
 */
public class ClusteringFactory {
    IClusteringConf conf;
    private Cluster cluster;

    private IClusteringConf.OperationMode overridingOperationMode;
    private boolean OverrideOperationMode;
    public void setOverrideOperationMode(IClusteringConf.OperationMode  mode)
    {
        overridingOperationMode = mode;
        OverrideOperationMode = true;
    }
    public ClusteringFactory(IClusteringConf conf)
    {
        this.conf = conf;
        OverrideOperationMode = false;
    }
    public IClusteringRepository getRepository()
    {
        IClusteringConf.OperationMode opmode = (OverrideOperationMode) ? overridingOperationMode : conf.getOperationMode();
        if( opmode == IClusteringConf.OperationMode.PARALLEL)
            return getParallelRepository();
        else if(opmode == IClusteringConf.OperationMode.DISTRIBUTED)
            return getDistributedRepository();
        else
        {
            System.err.println("ClusteringFactory: Undefined repository mode :" + conf.getOperationMode().toString());
        }
        return null;
    }
    IClusteringRepository getParallelRepository()
    {
        ClusteringCassandraRepository repository = null;
        Session session = initCassandraSession();
        repository = new ClusteringCassandraRepository(session,conf);
        return repository;
    }

    private Session initCassandraSession()
    {
        String[] hosts = conf.getCassandraHosts();
        if (hosts.length == 1) {
            this.cluster = Cluster
                    .builder()
                    .addContactPoint(hosts[0])
                    .withPort(conf.getCassandraPort())
                    .withClusterName(conf.getCassandraClusterName())
                    .build();
        } else {
            this.cluster = Cluster
                    .builder()
                    .addContactPoints(hosts)
                    .withPort(conf.getCassandraPort())
                    .withClusterName(conf.getCassandraClusterName())
                    .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
                    .withLoadBalancingPolicy(
                            Policies.defaultLoadBalancingPolicy())
                    .build();
        }
        Session session = cluster.connect(conf.getCassandraKeyspace());
        System.out.println("connected to: " + session.getState().getConnectedHosts().toString());
        return session;
    }
    IClusteringRepository getDistributedRepository()
    {
        Session session = initCassandraSession();
        return new ClusteringCassandraSparkRepository(session,conf);
    }
    public void releaseResources() {
        if(conf.getOperationMode() == IClusteringConf.OperationMode.PARALLEL) {
            if (cluster != null) {
                cluster.close();
            }
        }
        else if(conf.getOperationMode() == IClusteringConf.OperationMode.DISTRIBUTED)
        {
            ;
        }


    }



}
