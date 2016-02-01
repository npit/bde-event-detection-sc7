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
package gr.demokritos.iit.crawler.factory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import com.mchange.v2.c3p0.AbstractComboPooledDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import gr.demokritos.iit.crawler.schedule.DefaultScheduleLoader;
import gr.demokritos.iit.crawler.event.EventSink;
import gr.demokritos.iit.crawler.Fetcher;
import gr.demokritos.iit.model.Item;
import gr.demokritos.iit.crawler.event.LoggingEventSink;
import gr.demokritos.iit.crawlers.exceptions.UndeclaredRepositoryException;
import gr.demokritos.iit.repository.CassandraRepository;
import gr.demokritos.iit.repository.MySqlRepository;
import gr.demokritos.iit.repository.IRepository;
import java.beans.PropertyVetoException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;
import javax.sql.DataSource;

/**
 *
 *
 * A class which creates all the high-level and configurable components of the
 * system
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class SystemFactory {

    private final int maxHttpConnections;
    private final int maxQueueSize;
    private final int maxNumberOfCrawlingThreads;
    private final String urlsFileName;
    private final IRSSConf conf;
    private Cluster cluster;
    private AbstractComboPooledDataSource cpds = null;

    public SystemFactory(IRSSConf configuration) {
        this.conf = configuration;
        this.maxHttpConnections = configuration.getMaxHttpConnections();
        this.maxQueueSize = configuration.getMaxQueueSize();
        this.maxNumberOfCrawlingThreads = configuration.getMaxNumberOfCrawlingThreads();
        this.urlsFileName = configuration.getUrlsFileName();
    }

    public SystemFactory() {
        //Use the default settings
        this(new RSSConf());
    }

    public HttpClient createHttpClient() {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, maxHttpConnections);

        HttpProtocolParams.setUserAgent(params, Fetcher.USER_AGENT);
        ConnManagerParams.setTimeout(params, conf.getHttpTimeoutInSeconds());

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        ClientConnectionManager clientConnManager = new ThreadSafeClientConnManager(params, schemeRegistry);

        DefaultHttpClient httpClient = new DefaultHttpClient(clientConnManager, params);
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, conf.getHttpTimeoutInSeconds() * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, conf.getHttpTimeoutInSeconds() * 1000);
        return httpClient;
    }

    public EventSink createEventSink() {
        return new LoggingEventSink();
    }

    public BlockingQueue<Item> createBlockingQueue() {
        return new LinkedBlockingDeque(maxQueueSize);
    }

    public ExecutorService createConsumerExecutorService() {
        //Create service with  maxNumberOfCrawlingThreads  + 1 for the Consumer
        return Executors.newFixedThreadPool(maxNumberOfCrawlingThreads + 1);
    }

    public DefaultScheduleLoader createScheduleLoader(EventSink eventSink) {
        File file = new File(urlsFileName);
        return new DefaultScheduleLoader(file, eventSink);
    }

    public ScheduledExecutorService createProducerExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    public ExecutorService createOneTimeProducerExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

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
                System.out.println(session.getState().getConnectedHosts().toString());
                repository = new CassandraRepository(session);
                break;
            case MYSQL:
                dataSource = initializeSQLDataSource();
                repository = MySqlRepository.createBlogRepository(dataSource, conf.getDatabaseName());
                break;
        }
        return repository;
    }

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
                System.out.println(session.getState().getConnectedHosts().toString());
                repository = new CassandraRepository(session);
                break;
            case MYSQL:
                dataSource = initializeSQLDataSource();
                repository = MySqlRepository.createNewsRepository(dataSource, conf.getDatabaseName());
                break;
        }
        return repository;
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

        MYSQL(MySqlRepository.class.getName()), CASSANDRA(CassandraRepository.class.getName());

        private final String impl_class_decl;

        private Repository(String impl_class_decl) {
            this.impl_class_decl = impl_class_decl;
        }

        public static final EnumMap<Repository, String> REPOSITORY_DECLARATIONS = new EnumMap(Repository.class);

        static {
            REPOSITORY_DECLARATIONS.put(MYSQL, MySqlRepository.class.getName());
            REPOSITORY_DECLARATIONS.put(CASSANDRA, CassandraRepository.class.getName());
        }
    }

    private DataSource initializeSQLDataSource() throws PropertyVetoException {
        cpds = new ComboPooledDataSource();
        cpds.setDriverClass("com.mysql.jdbc.Driver");
        cpds.setJdbcUrl("jdbc:" + conf.getDatabaseHost());
        cpds.setUser(conf.getDatabaseUserName());
        cpds.setPassword(conf.getDatabasePassword());

        // the settings below are optional -- c3p0 can work with defaults
        cpds.setMinPoolSize(conf.getDataSourceMinPoolSize());
        cpds.setAcquireIncrement(conf.getDataSourceAcquireIncrement());
        cpds.setMaxPoolSize(conf.getDataSourceMaxPoolSize());
        cpds.setMaxStatements(conf.getDataSourceMaxStatements());
        return cpds;
    }
}
