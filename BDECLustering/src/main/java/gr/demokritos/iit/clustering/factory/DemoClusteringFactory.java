package gr.demokritos.iit.clustering.factory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.IBaseRepository;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 * @date 4/13/16
 */
public class DemoClusteringFactory {

    private final IBaseConf conf;
    private Cluster cluster;

    public DemoClusteringFactory(IBaseConf conf) {
        this.conf = conf;
    }


    public DemoCassandraRepository createDemoCassandraRepository() {
        DemoCassandraRepository repository = null;
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
        repository = new DemoCassandraRepository(session);
        return repository;
    }

}
