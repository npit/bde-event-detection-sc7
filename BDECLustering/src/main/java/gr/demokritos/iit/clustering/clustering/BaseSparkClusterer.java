package gr.demokritos.iit.clustering.clustering;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingPairsFunc;
import gr.demokritos.iit.clustering.newsum.IClusterer;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.model.structures.URLImage;
import scala.Tuple2;
import scala.Tuple4;

import java.util.*;
import java.util.logging.Level;

/**
 * Created by npittaras on 16/8/2016.
 */
public class BaseSparkClusterer implements IClusterer {

    private final SparkContext sc;
    private final SimilarityMode mode;
    private final double simCutOff;
    private final int numPartitions;
    private List<Topic> Clusters;

    protected Map<String, Topic> ArticlesPerCluster;
    protected Map<Article, String> ClustersPerArticle;


    public BaseSparkClusterer(SparkContext scArg, SimilarityMode mode, double simCutOff, int numPartitions) {
        this.sc = scArg;
        this.mode = mode;
        this.simCutOff = simCutOff;
        this.numPartitions = numPartitions;
    }

    /**
     *
     * @param articles expects an RDD of <URL, title, text, timestamp>
     *
     *  Clusters the articles in a naive way, based on scify BaseArticleClusterer.
     */
    @Override
    public void calculateClusters(JavaRDD<Tuple4<String, String, String, Long>> articles) {

        // create pairs
        System.out.print("Generating article pairs...");
        JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs
                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());
        // debug
        StructUtils.printArticlePairs(RDDPairs, 5);
        // get matching mapping

        // TODO: use flatMap?? we want for the full pairs rdd, each item mapped to a boolean value.
        JavaRDD<Boolean> matchesrdd = RDDPairs.map(new ExtractMatchingPairsFunc(sc, mode, simCutOff, numPartitions));

        // spark parallelization ends here.
        // collect matches values
        System.out.println("Appid : " + sc.applicationId().toString() );
        List<Boolean> matches = matchesrdd.collect();

        // generate clusters
        // baseArticleClusterer shuts down executors. Should we collect the data and do that here too?

        // loop on the pairs
        ArticlesPerCluster = new HashMap<String,Topic>();
        ClustersPerArticle = new HashMap<Article,String>();

        int count = -1;
        boolean matchValue;
        // foreach pair
        for(Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> pair : RDDPairs.collect())
        {
            ++ count; // pair index
            matchValue = matches.get(count);
            String clusterID;
            Topic newTopic;
            // 4-tuple : entry url, title, clean text, published date
            // get article entities
            Article article1 = new Article(pair._1()._1(),pair._1()._2(),pair._1()._3(),"","",new URLImage(""),new Date(pair._1()._4()));
            Article article2 = new Article(pair._2()._1(),pair._2()._2(),pair._2()._3(),"","",new URLImage(""),new Date(pair._2()._4()));

            //Article first = pair.
            if(matchValue) // if the article pair matches
            {
                // if both exist in a cluster, merge the clusters
                if(ClustersPerArticle.containsKey(article1) && ClustersPerArticle.containsKey(article2)) {
                    this.collapseTopics((String)this.ClustersPerArticle.get(article1), (String)this.ClustersPerArticle.get(article2));
                }
                else // one or more articles is not in any cluster
                {
                    // if first article not in cluster, create one and enter it
                    if(! ClustersPerArticle.containsKey(article1))
                    {
                        Topic tNew = new Topic();
                        String sClusterID = tNew.getID();
                        tNew.add(article1);
                        ArticlesPerCluster.put(sClusterID, tNew);
                        ClustersPerArticle.put(article1, sClusterID);
                    }
                    // check the second article
                    if( ClustersPerArticle.containsKey(article2))
                    {   // if  article2 is in a cluster, merge the
                        this.collapseTopics((String)this.ClustersPerArticle.get(article1), (String)this.ClustersPerArticle.get(article2));
                    }
                    else    // second not contained
                    {
                        // create new cluster
                        Topic tNew = new Topic();
                        String sClusterID = tNew.getID();
                        ArticlesPerCluster.put(sClusterID, tNew);

                        ((Topic)ArticlesPerCluster.get(sClusterID)).add(article2);
                        ClustersPerArticle.put(article2, sClusterID);
                    }
                }


            }
            else  // if they do not match
            {
                // create new clusters  for each article

                if( !ClustersPerArticle.containsKey(article1))
                {
                    Topic tNew = new Topic();
                    String sClusterID = tNew.getID();
                    tNew.add(article1);
                    ArticlesPerCluster.put(sClusterID, tNew);
                    ClustersPerArticle.put(article1, sClusterID);
                }
                if( !ClustersPerArticle.containsKey(article2))
                {
                    Topic tNew = new Topic();
                    String sClusterID = tNew.getID();
                    tNew.add(article2);
                    ArticlesPerCluster.put(sClusterID, tNew);
                    ClustersPerArticle.put(article2, sClusterID);
                }
            }
        }

        checkForInconsistencies();
        //generateFinalTopics();



        // create triple-tuples <pair1, pair2, matchOrNot> ?

        // TODO: change method signature: return smth (not void)


    }

    protected boolean collapseTopics(String sTopic1ID, String sTopic2ID) {
        Topic t1 = (Topic)this.ArticlesPerCluster.get(sTopic1ID);
        Topic t2 = (Topic)this.ArticlesPerCluster.get(sTopic2ID);
        if(t1 == t2) {
            return false;
        } else {
            Iterator i$ = t2.iterator();

            while(i$.hasNext()) {
                Article aCur = (Article)i$.next();
                t1.add(aCur);
                this.ClustersPerArticle.put(aCur, t1.getID());
                this.ArticlesPerCluster.put(t1.getID(), t1);
            }

            t2.clear();
            this.ArticlesPerCluster.remove(t2.getID());
            return true;
        }
    }

    protected void checkForInconsistencies() {
        int iCnt = 0;

        Iterator i$;
        for(i$ = this.ClustersPerArticle.keySet().iterator(); i$.hasNext(); ++iCnt) {
            Article sCurCluster = (Article)i$.next();
            if(!((Topic)this.ArticlesPerCluster.get(this.ClustersPerArticle.get(sCurCluster))).contains(sCurCluster)) {
                System.out.println( "Mismatch found!");
            }
        }

        System.out.println(String.format("Checked {0} items.",  Integer.valueOf(iCnt)));
        i$ = this.ArticlesPerCluster.keySet().iterator();

        while(i$.hasNext()) {
            String var6 = (String)i$.next();
            Iterator i$1 = ((Topic)this.ArticlesPerCluster.get(var6)).iterator();

            while(i$1.hasNext()) {
                Article aCurArticle = (Article)i$1.next();
                if(((String)this.ClustersPerArticle.get(aCurArticle)).trim().compareTo(var6.trim()) != 0) {
                    System.out.println(String.format("Mismatch found (reverse)!\n{0} != \n{1}\n",  new Object[]{this.ClustersPerArticle.get(aCurArticle), var6}));
                }
            }
        }

        System.out.println( "Reversed Checked Mappings Done");
    }

}