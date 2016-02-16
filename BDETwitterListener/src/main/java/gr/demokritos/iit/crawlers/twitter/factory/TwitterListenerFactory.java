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
package gr.demokritos.iit.crawlers.twitter.factory;

import gr.demokritos.iit.crawlers.twitter.factory.conf.ITwitterConf;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import com.mchange.v2.c3p0.AbstractComboPooledDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import gr.demokritos.iit.base.exceptions.UndeclaredRepositoryException;
import gr.demokritos.iit.crawlers.twitter.factory.conf.TConfig;
import gr.demokritos.iit.crawlers.twitter.impl.ITwitterRestConsumer;
import gr.demokritos.iit.crawlers.twitter.impl.BaseTwitterRestConsumer;
import gr.demokritos.iit.crawlers.twitter.policy.ICrawlPolicy;
import gr.demokritos.iit.crawlers.twitter.policy.InfluentialCrawlPolicy;
import gr.demokritos.iit.crawlers.twitter.repository.CassandraRepository;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import gr.demokritos.iit.crawlers.twitter.repository.MongoRepository;
import gr.demokritos.iit.crawlers.twitter.repository.MySQLRepository;
import gr.demokritos.iit.crawlers.twitter.stream.IStreamConsumer;
import gr.demokritos.iit.crawlers.twitter.stream.SampleStatusListener;
import gr.demokritos.iit.crawlers.twitter.stream.user.UserStatusListener;
import gr.demokritos.iit.crawlers.twitter.url.DefaultURLUnshortener;
import gr.demokritos.iit.crawlers.twitter.url.IURLUnshortener;
import java.beans.PropertyVetoException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Map;
import javax.sql.DataSource;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class TwitterListenerFactory implements ITwitterFactory {

    private final ITwitterConf conf;

    private Cluster cluster = null;
    private AbstractComboPooledDataSource cpds = null;

    public TwitterListenerFactory(ITwitterConf conf) {
        this.conf = conf;
    }

    @Override
    public ICrawlPolicy getCrawlPolicy(IRepository repository)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        ICrawlPolicy policy;
        String policy_decl = conf.getCrawlPolicy();
        Class sourceClass = Class.forName(policy_decl);

        // if influential crawl policy
        if (policy_decl.equals(InfluentialCrawlPolicy.class.getName())) {
            // get followers cut-off threshold, and instantiate
            Constructor class_constructor = sourceClass.getConstructor(IRepository.class, long.class);
            policy = (ICrawlPolicy) class_constructor.newInstance(repository, conf.getFollowersCutOff());
        } else {
            Constructor class_constructor = sourceClass.getConstructor(IRepository.class);
            policy = (ICrawlPolicy) class_constructor.newInstance(repository);
        }
        return policy;
    }

    /**
     * switch only repository, others use default
     *
     * @param repository
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Override
    public ITwitterRestConsumer getTwitterListener(IRepository repository)
            throws
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        ITwitterRestConsumer crawler;
        String crawl_decl = conf.getCrawlerImpl();
        Class sourceClass = Class.forName(crawl_decl);
        Constructor class_constructor = sourceClass.getConstructor(ITwitterConf.class, IRepository.class, ICrawlPolicy.class);
        ICrawlPolicy policy = getCrawlPolicy(repository);
        crawler = (ITwitterRestConsumer) class_constructor.newInstance(conf, repository, policy);
        return crawler;
    }

    /**
     * load twitter listener (the implementation provided at
     * 'twitter.properties'), loads all declared implementations of:
     * {@link IRepository}, {@link ICrawlPolicy} along with the {@link TConfig}
     *
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws PropertyVetoException
     */
    @Override
    public ITwitterRestConsumer getTwitterListener()
            throws
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            PropertyVetoException {
        ITwitterRestConsumer crawler;
        String crawl_decl = conf.getCrawlerImpl();
        Class sourceClass = Class.forName(crawl_decl);
        Constructor class_constructor = sourceClass.getConstructor(ITwitterConf.class, IRepository.class, ICrawlPolicy.class);
        IRepository repository = getRepository();
        ICrawlPolicy policy = getCrawlPolicy(repository);
        crawler = (ITwitterRestConsumer) class_constructor.newInstance(conf, repository, policy);
        return crawler;
    }

    /**
     * used for search instantiation
     *
     * @return an instance of {@link BaseTwitterRestConsumer}
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws PropertyVetoException
     */
    @Override
    public ITwitterRestConsumer getBaseTwitterListener()
            throws
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            PropertyVetoException {
        ITwitterRestConsumer crawler;
        Class sourceClass = Class.forName(BaseTwitterRestConsumer.class.getName());
        Constructor class_constructor = sourceClass.getConstructor(ITwitterConf.class, IRepository.class, ICrawlPolicy.class);
        IRepository repository = getRepository();
        ICrawlPolicy policy = getCrawlPolicy(repository);
        crawler = (ITwitterRestConsumer) class_constructor.newInstance(conf, repository, policy);
        return crawler;
    }

    /**
     * get an implementation class of {@link IURLUnshortener}. The URL expander
     * is utilized when we want to expand URLs from links of tweets
     *
     * @return
     */
    @Override
    public IURLUnshortener getDefaultURLUnshortener() {
        IURLUnshortener unshort = new DefaultURLUnshortener(
                conf.getConnectionTimeOut(),
                conf.getSocketTimeout(),
                conf.getCacheSize());
        return unshort;
    }

    /**
     * used for search instantiation
     *
     * @return an instance of {@link BaseTwitterRestConsumer}
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Override
    public IURLUnshortener getURLUnshortener() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String unshort_decl = conf.getURLUnshortenerImpl();
        Class sourceClass = Class.forName(unshort_decl);
        Constructor class_constructor = sourceClass.getConstructor(int.class, int.class, int.class);
        return (IURLUnshortener) class_constructor.newInstance(
                conf.getConnectionTimeOut(),
                conf.getSocketTimeout(),
                conf.getCacheSize());
    }

    /**
     * get an implementation class of {@link IRepository}. Supported
     * repositories are Cassandra, MySQL.
     *
     * @return
     * @throws PropertyVetoException
     * @throws UndeclaredRepositoryException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Override
    public IRepository getRepository() throws PropertyVetoException, UndeclaredRepositoryException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        IRepository repository;
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
                            // ISSUE: with streaming API, the 2.1.9 driver cannot connect to cassandra (possibly cause of underlying workers)
                            // com.datastax.driver.core.exceptions.NoHostAvailableException: All host(s) tried for query failed (no host was tried)
                            // for that reason, we switched to the 3.0.0-rc1 (current latest stable) driver.
                            // Not occuring on serial inserts though (rest API).
                            .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
                            .withLoadBalancingPolicy(
                                    Policies.defaultLoadBalancingPolicy())
                            //                                new TokenAwarePolicy(new RoundRobinPolicy()))
                            //                                new TokenAwarePolicy(new DCAwareRoundRobinPolicy()))
                            .build();
                }
//                System.out.println(cluster.getConfiguration().getPoolingOptions().toString());
//                System.out.println(cluster.getMetadata().getAllHosts().toString());
                Session session = cluster.connect(conf.getCassandraKeyspace());
                System.out.println(session.getState().getConnectedHosts().toString());
                repository = new CassandraRepository(session, getURLUnshortener());
                break;
            case MYSQL:
                dataSource = initializeSQLDataSource();
                repository = new MySQLRepository(dataSource, getURLUnshortener());
                break;
            default: // default mysql
                dataSource = initializeSQLDataSource();
                repository = new MySQLRepository(dataSource, getURLUnshortener());
                break;
        }
        return repository;
    }

    /**
     * get an implementation class of {@link IStreamConsumer}. Currently, the
     * only meaningful implementation is {@link UserStatusListener}
     *
     * @return
     * @throws PropertyVetoException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws UndeclaredRepositoryException
     * @throws InvocationTargetException
     */
    @Override
    public IStreamConsumer getStreamImpl() throws PropertyVetoException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, UndeclaredRepositoryException, InvocationTargetException {
        IStreamConsumer cons;
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(conf.getTwitterConsumerKey())
                .setOAuthConsumerSecret(conf.getTwitterConsumerKeySecret())
                .setOAuthAccessToken(conf.getTwitterAccessTokken())
                .setOAuthAccessTokenSecret(conf.getTwitterAccessTokkenSecret());
        TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
        TwitterStream streamreader = tf.getInstance();
        IRepository repos = getRepository();
        String stream_impl_decl = conf.getStreamImpl();
        Class sourceClass = Class.forName(stream_impl_decl);
        Constructor class_constructor;
        // if public sample, get lang wanted (default 'en');
        if (stream_impl_decl.equalsIgnoreCase(SampleStatusListener.class.getName())) {
            class_constructor = sourceClass.getConstructor(TwitterStream.class, IRepository.class, String.class);
            cons = (IStreamConsumer) class_constructor.newInstance(streamreader, repos, conf.getStreamLanguage());
        } else {
            class_constructor = sourceClass.getConstructor(TwitterStream.class, IRepository.class);
            cons = (IStreamConsumer) class_constructor.newInstance(streamreader, repos);
        }
        return cons;
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

        MYSQL(MySQLRepository.class.getName()), CASSANDRA(CassandraRepository.class.getName()), MONGO(MongoRepository.class.getName());

        private final String impl_class_decl;

        private Repository(String impl_class_decl) {
            this.impl_class_decl = impl_class_decl;
        }

        public static final EnumMap<Repository, String> REPOSITORY_DECLARATIONS = new EnumMap(Repository.class);

        static {
            REPOSITORY_DECLARATIONS.put(MYSQL, MySQLRepository.class.getName());
            REPOSITORY_DECLARATIONS.put(CASSANDRA, CassandraRepository.class.getName());
            REPOSITORY_DECLARATIONS.put(MONGO, MongoRepository.class.getName());
        }
    }
}
