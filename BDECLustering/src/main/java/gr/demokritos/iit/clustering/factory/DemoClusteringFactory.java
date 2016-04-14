package gr.demokritos.iit.clustering.factory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.repository.BaseCassandraRepository;
import gr.demokritos.iit.base.repository.IBaseRepository;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;
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

    public IArticleClusterer getSocialMediaClustererForTwitter(SocialMediaClusterer.Mode metrics_mode, CleanResultCollection<TwitterResult> tweets) {
        if (tweets == null) {
            return null;
        }
        SocialMediaClusterer.SocialMediaClustererBuilder builder
                = new SocialMediaClusterer.SocialMediaClustererBuilder()
                // set mode for clusterer
                .withMode(metrics_mode)
                .withDefaultSimilarityThreshold()
                .withDefaultSizeThreshold()
                .withTweets(tweets);
        // create clusterer
        return builder.build();
    }

    public IClassifier getSocialMediaClassifierForTwitter(
            Map<String, String> plainSummaries,
            Collection<Topic> clusters,
            IStemmer stemmer
    ) {
        // use Default Thresholds
        return new SocialMediaClassifier(plainSummaries, clusters, stemmer);
    }

    public void releaseResources() {
        if (cluster !=null) { cluster.close(); }
    }
}
