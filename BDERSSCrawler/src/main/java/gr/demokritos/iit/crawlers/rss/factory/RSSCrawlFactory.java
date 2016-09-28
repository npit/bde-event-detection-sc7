/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.crawlers.rss.factory;

import gr.demokritos.iit.crawlers.rss.factory.conf.IRSSConf;
import gr.demokritos.iit.crawlers.rss.factory.conf.RSSConf;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import com.mchange.v2.c3p0.AbstractComboPooledDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import gr.demokritos.iit.crawlers.rss.schedule.DefaultScheduleLoader;
import gr.demokritos.iit.crawlers.rss.event.EventSink;
import gr.demokritos.iit.crawlers.rss.model.Item;
import gr.demokritos.iit.crawlers.rss.event.LoggingEventSink;
import gr.demokritos.iit.base.exceptions.UndeclaredRepositoryException;
import gr.demokritos.iit.crawlers.rss.Fetcher;
import gr.demokritos.iit.crawlers.rss.repository.CassandraRepository;
import gr.demokritos.iit.crawlers.rss.repository.MySqlRepository;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;
import gr.demokritos.iit.crawlers.rss.repository.InMemoryRepository;
import java.beans.PropertyVetoException;
import org.apache.http.client.HttpClient;

import java.io.File;
import java.nio.charset.CodingErrorAction;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.apache.http.Consts;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 *
 *
 * A class which creates all the high-level and configurable components of the
 * system
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class RSSCrawlFactory implements IRSSFactory {

    private final int maxHttpConnections;
    private final int maxQueueSize;
    private final int maxNumberOfCrawlingThreads;
    private final String urlsFileName;
    private final IRSSConf conf;
    private Cluster cluster;
    private AbstractComboPooledDataSource cpds = null;

    public RSSCrawlFactory(IRSSConf configuration) {
        this.conf = configuration;
        this.maxHttpConnections = configuration.getMaxHttpConnections();
        this.maxQueueSize = configuration.getMaxQueueSize();
        this.maxNumberOfCrawlingThreads = configuration.getMaxNumberOfCrawlingThreads();
        this.urlsFileName = configuration.getUrlsFileName();
    }

    public RSSCrawlFactory() {
        // use default settings
        this(new RSSConf());
    }

//    public HttpClient createHttpClient() {
//        HttpParams params = new BasicHttpParams();
//        ConnManagerParams.setMaxTotalConnections(params, maxHttpConnections);
//
//        HttpProtocolParams.setUserAgent(params, Fetcher.USER_AGENT);
//        ConnManagerParams.setTimeout(params, conf.getHttpTimeoutInSeconds());
//
//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
//
//        ClientConnectionManager clientConnManager = new ThreadSafeClientConnManager(params, schemeRegistry);
//
//        DefaultHttpClient httpClient = new DefaultHttpClient(clientConnManager, params);
//        HttpParams httpParams = httpClient.getParams();
//        HttpConnectionParams.setConnectionTimeout(httpParams, conf.getHttpTimeoutInSeconds() * 1000);
//        HttpConnectionParams.setSoTimeout(httpParams, conf.getHttpTimeoutInSeconds() * 1000);
//        return httpClient;
//    }
    @Override
    public HttpClient createHttpClient() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .build();
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .setCharset(Consts.UTF_8)
                .build();
        // Configure the connection manager to use connection configuration 
        connManager.setDefaultConnectionConfig(connectionConfig);

        // Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        connManager.setMaxTotal(maxHttpConnections);
        connManager.setValidateAfterInactivity(conf.getHttpTimeoutInSeconds() * 1000);
        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setExpectContinueEnabled(true)
                .setSocketTimeout(conf.getSocketTimeout())
                .setConnectTimeout(conf.getConnectionTimeOut())
                .setConnectionRequestTimeout(conf.getConnectionTimeOut())
                .build();

        // Create an HttpClient with the given custom dependencies and configuration.
        HttpClient httpclient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(defaultRequestConfig)
                .setUserAgent(Fetcher.USER_AGENT)
                .build();
        return httpclient;
    }

    @Override
    public EventSink createEventSink() {
        return new LoggingEventSink();
    }

    @Override
    public BlockingQueue<Item> createBlockingQueue() {
        return new LinkedBlockingDeque(maxQueueSize);
    }

    @Override
    public ExecutorService createConsumerExecutorService() {
        //Create service with  maxNumberOfCrawlingThreads  + 1 for the Consumer
        return Executors.newFixedThreadPool(maxNumberOfCrawlingThreads + 1);
    }

    @Override
    public DefaultScheduleLoader createScheduleLoader(EventSink eventSink) {
        File file = new File(urlsFileName);
        return new DefaultScheduleLoader(file, eventSink);
    }

    @Override
    public ScheduledExecutorService createProducerExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public ExecutorService createOneTimeProducerExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Override
    public IRepository createBlogRepository() throws PropertyVetoException {
        IRepository repository = null;
        String backend = conf.getRepositoryImpl().trim();
        Repository repo_type;
        repo_type = getRepositoryType(backend);
        DataSource dataSource;
        switch (repo_type) {
            case CASSANDRA:
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
                repository = CassandraRepository.createBlogRepository(session);
                break;
            case MYSQL:
                dataSource = initializeSQLDataSource();
                repository = MySqlRepository.createBlogRepository(dataSource, conf.getDatabaseName());
                break;
        }
        return repository;
    }

    @Override
    public IRepository createNewsRepository() throws PropertyVetoException {
        IRepository repository = null;
        String backend = conf.getRepositoryImpl().trim();
        Repository repo_type;
        repo_type = getRepositoryType(backend);
        DataSource dataSource;
        switch (repo_type) {
            case CASSANDRA:
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
                repository = CassandraRepository.createNewsRepository(session);
                break;
            case MYSQL:
                dataSource = initializeSQLDataSource();
                repository = MySqlRepository.createNewsRepository(dataSource, conf.getDatabaseName());
                break;
            case INRAM:
//                dataSource = initializeSQLDataSource();
                repository = new InMemoryRepository();
                break;
        }
        return repository;
    }

    /**
     * release underlying DB connection pools
     */
    @Override
    public void releaseResources() {
        if (cluster != null) {
            cluster.close();
        }
        if (cpds != null) {
            cpds.close();
        }
    }

    private Repository getRepositoryType(String backend) throws UndeclaredRepositoryException {
        for (Map.Entry<Repository, String> repo : Repository.REPOSITORY_DECLARATIONS.entrySet()) {
            String val = repo.getValue();
            if (backend.equalsIgnoreCase(val)) {
                return repo.getKey();
            }
        }
        throw new UndeclaredRepositoryException();
    }

    /**
     * strict repository declarations
     */
    public enum Repository {

        MYSQL(MySqlRepository.class.getName()), CASSANDRA(CassandraRepository.class.getName()), INRAM(InMemoryRepository.class.getName());

        private final String impl_class_decl;

        private Repository(String impl_class_decl) {
            this.impl_class_decl = impl_class_decl;
        }

        public static final EnumMap<Repository, String> REPOSITORY_DECLARATIONS = new EnumMap(Repository.class);

        static {
            REPOSITORY_DECLARATIONS.put(MYSQL, MySqlRepository.class.getName());
            REPOSITORY_DECLARATIONS.put(CASSANDRA, CassandraRepository.class.getName());
            REPOSITORY_DECLARATIONS.put(INRAM, InMemoryRepository.class.getName());
        }
    }

    private DataSource initializeSQLDataSource() throws PropertyVetoException {
        cpds = new ComboPooledDataSource();
        cpds.setDriverClass("com.mysql.jdbc.Driver");
        cpds.setJdbcUrl("jdbc:" + conf.getDatabaseHost());
        cpds.setUser(conf.getDatabaseUserName());
        cpds.setPassword(conf.getDatabasePassword());

        System.out.println("host:["+cpds.getJdbcUrl()+"]");
        System.out.println("user:["+cpds.getUser()+"]");
        System.out.println("pwd:["+cpds.getPassword()+"]");

        // the settings below are optional -- c3p0 can work with defaults
        cpds.setMinPoolSize(conf.getDataSourceMinPoolSize());
        cpds.setAcquireIncrement(conf.getDataSourceAcquireIncrement());
        cpds.setMaxPoolSize(conf.getDataSourceMaxPoolSize());
        cpds.setMaxStatements(conf.getDataSourceMaxStatements());
        return cpds;
    }
}
