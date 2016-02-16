/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.factory;

import gr.demokritos.iit.location.factory.conf.ILocConf;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import gr.demokritos.iit.location.extraction.DefaultLocationExtractor;
import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.extraction.provider.ITokenProvider;
import gr.demokritos.iit.location.mapping.DefaultPolygonExtraction;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.repository.LocationCassandraRepository;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.location.sentsplit.ISentenceSplitter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocationFactory implements ILocFactory {

    protected ILocConf conf;
    protected Cluster cluster;

    public LocationFactory(ILocConf conf) {
        this.conf = conf;
    }

    @Override
    public ILocationRepository createLocationCassandraRepository() {
        ILocationRepository repository;
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
        repository = new LocationCassandraRepository(session);
        return repository;
    }

    @Override
    public IPolygonExtraction createDefaultPolygonExtractionClient() throws IllegalArgumentException {
        String url = conf.getPolygonExtractionURL();
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("should provide 'polygon_extraction_url' in properties file."));
        }
        return new DefaultPolygonExtraction(url);
    }

    /**
     * release underlying DB connection pools
     */
    @Override
    public void releaseResources() {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Override
    public ILocationExtractor createDefaultLocationExtractor() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ITokenProvider tp = createTokenProvider();
        return new DefaultLocationExtractor(tp);
    }

    @Override
    public ILocationExtractor createLocationExtractor() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ITokenProvider tp = createTokenProvider();
        String locExImpl = conf.getLocationExtractionImpl();
        Class tpSourceCl = Class.forName(locExImpl);
        Constructor class_constructor;
        class_constructor = tpSourceCl.getConstructor(ITokenProvider.class);
        return (ILocationExtractor) class_constructor.newInstance(tp);
    }

    @Override
    public ITokenProvider createTokenProvider() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ISentenceSplitter splitter = createSentenceSplitter();
        String tp_ipml_decl = conf.getTokenProviderImpl();
        Class tpSourceCl = Class.forName(tp_ipml_decl);
        Constructor class_constructor;
        class_constructor = tpSourceCl.getConstructor(String.class, ISentenceSplitter.class, double.class);
        return (ITokenProvider) class_constructor.newInstance(conf.getNEModelsDirPath(), splitter, conf.getNEConfidenceCutOffThreshold());
    }

    @Override
    public ISentenceSplitter createSentenceSplitter() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String sent_split_decl = conf.getSentenceSplitterImpl();
        Class sentSplitSourceCl = Class.forName(sent_split_decl);
        Constructor class_constructor;
        class_constructor = sentSplitSourceCl.getConstructor(String.class);
        return (ISentenceSplitter) class_constructor.newInstance(conf.getSentenceSplitterModelPath());
    }
}
