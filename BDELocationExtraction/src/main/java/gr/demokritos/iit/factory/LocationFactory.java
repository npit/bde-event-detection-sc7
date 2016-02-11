/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.factory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.factory.BaseFactory;
import gr.demokritos.iit.location.repository.CassandraLocationRepository;
import gr.demokritos.iit.location.repository.ILocationRepository;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocationFactory extends BaseFactory {

    public LocationFactory(IBaseConf conf) {
        super(conf);
    }

    public ILocationRepository createLocationCassandraRepository() {
        ILocationRepository repository = null;
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
        repository = new CassandraLocationRepository(session);
        return repository;
    }

}
