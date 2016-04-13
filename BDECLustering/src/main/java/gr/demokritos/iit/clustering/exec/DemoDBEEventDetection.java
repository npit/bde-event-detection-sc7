package gr.demokritos.iit.clustering.exec;

import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.repository.DemoCassandraRepository;
import org.scify.newsum.server.clustering.BaseArticleClusterer;
import org.scify.newsum.server.clustering.IArticleClusterer;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.nlp.sentsplit.DefaultSentenceSplitter;
import org.scify.newsum.server.nlp.sentsplit.ISentenceSplitter;
import org.scify.newsum.server.nlp.sentsplit.OpenNLPSentenceSplitter;
import org.scify.newsum.server.summarization.BaseSummariser;
import org.scify.newsum.server.summarization.ISummarizer;
import org.scify.newsum.server.summarization.Summarizer;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class DemoDBEEventDetection {

    /**
     *
     * @param args provide the .properties file (with cassandra credentials, etc).
     */
    public static void main(String[] args) {

        String properties = "./res/clustering.properties";
        if (args.length == 1) {
            properties = args[0];
        }
        IBaseConf conf = new BaseConfiguration(properties);
        DemoClusteringFactory factory = new DemoClusteringFactory(conf);

        DemoCassandraRepository repository = factory.createDemoCassandraRepository();


        Calendar now = Calendar.getInstance();
        now.set(Calendar.MONTH, now.get(Calendar.MONTH) - 1);

        List<Article> articles = repository.loadArticlesAsDemo(now.getTimeInMillis());

        IArticleClusterer cl = new BaseArticleClusterer(articles);

        cl.calculateClusters();

        Map<String, Topic> clusters = cl.getArticlesPerCluster();

        ISentenceSplitter splitter = new DefaultSentenceSplitter(conf.getSentenceSplitterModelPath());

        ISummarizer sum = new Summarizer(splitter);

        Map<String, Summary> summaries = sum.getSummaries(new HashSet(clusters.values()));

        repository.saveEvents(summaries);


    }

}
