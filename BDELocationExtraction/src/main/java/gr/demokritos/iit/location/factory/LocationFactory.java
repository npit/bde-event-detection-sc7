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
import gr.demokritos.iit.location.mapping.LocalPolygonExtraction;
import gr.demokritos.iit.location.mapping.client.IRestClient;
import gr.demokritos.iit.location.mapping.client.SimpleRestClient;
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

        //repository.setDataRetrievalWindow(conf.getRetrievalTimeWindow());
        return repository;
    }

    @Override
    public IPolygonExtraction createPolygonExtractionClient() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        String poly_extraction_impl = conf.getPolygonExtractionImpl();

        try {
            if (poly_extraction_impl.equals("local"))
                return createLocalPolygonExtractionClient();
            else if (poly_extraction_impl.equals("remote"))
                return createDefaultPolygonExtractionClient();
            else {
                throw new IllegalArgumentException(String.format("Undefined polygon extraction implementation: " + poly_extraction_impl + " . Available: local | remote."));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public IPolygonExtraction createDefaultPolygonExtractionClient() throws IllegalArgumentException {
        String url = conf.getPolygonExtractionURL();
        String impl = conf.getRestClientImpl();
        IRestClient client = null;
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("should provide 'polygon_extraction_url' in properties file."));
        }
        boolean Failed = false;
        try {
            if(!impl.isEmpty()) {
                Class C = Class.forName(impl);
                Constructor ctor = C.getConstructor();
                client = (IRestClient) ctor.newInstance();
            }
            else
	    {
                System.out.println("No rest implementation provided. Using Simple.");
		Failed = true;
	    }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Rest implementation " + impl + " is not defined. Using Simple.");
            Failed = true;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.out.println("Failed to get constructor for Rest implementation " + impl + ". Using Simple.");
            Failed = true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.out.println("IllegalAccessException for Rest implementation " + impl + ". Using Simple.");
            Failed = true;

        } catch (InstantiationException e) {
            e.printStackTrace();
            System.out.println("InstantiationExceptionfor Rest implementation " + impl + ". Using Simple.");
            Failed = true;

        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("InvocationTargetException for Rest implementation " + impl + ". Using Simple.");
            Failed = true;

        }

        if(Failed)
            client = new SimpleRestClient();

        return new DefaultPolygonExtraction(url,1000,client);
    }

    @Override
    public IPolygonExtraction createLocalPolygonExtractionClient() throws IllegalArgumentException {
        String sourcefile=conf.getPolygonExtractionSourceFile();
        try {
            return new LocalPolygonExtraction(sourcefile);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
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
