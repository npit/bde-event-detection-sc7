package gr.demokritos.iit.clustering.clustering;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingGraphPairsFunc;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingPairsFunc;
import gr.demokritos.iit.clustering.newsum.IClusterer;
import gr.demokritos.iit.clustering.structs.SimilarityMode;
import gr.demokritos.iit.clustering.util.DocumentPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.GraphPairGenerationFilterFunction;
import gr.demokritos.iit.clustering.util.StructUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.graphx.Graph;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.model.structures.URLImage;
import scala.Tuple2;
import scala.Tuple4;

import java.util.*;


import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map.Entry;

/**
 * Created by npittaras on 16/8/2016.
 */
public class BaseSparkClusterer implements IClusterer {

    private final SparkContext sc;
    private final SimilarityMode mode;
    private final double simCutOff;
    private final int numPartitions;
    private List<Topic> Clusters;

    public Map<String, Topic> ArticlesPerCluster;
    protected Map<String, String> ClustersPerArticle;


    public BaseSparkClusterer(SparkContext scArg, SimilarityMode mode, double simCutOff, int numPartitions) {
        this.sc = scArg;
        this.mode = mode;
        this.simCutOff = simCutOff;
        this.numPartitions = numPartitions;
    }

    public Map<String, Topic> getArticlesPerCluster()
    {
        return  ArticlesPerCluster;
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
        System.out.println("Generating article pairs...");
        JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs
                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());
        // debug
//        StructUtils.printArticlePairs(RDDPairs, 5);
        // get matching mapping
        System.out.println("Mapping to boolean similarity...");
        System.out.println("Calculate clusters, using " + mode + " " + simCutOff + " " + numPartitions);

        long startTime = System.currentTimeMillis();

        // TODO: use flatMap?? we want for the full pairs rdd, each item mapped to a boolean value.
        JavaRDD<Boolean> matchesrdd = RDDPairs.map(new ExtractMatchingPairsFunc(sc, mode, simCutOff, numPartitions));

        // spark parallelization ends here.
        // collect matches values



        List<Boolean> matches = matchesrdd.collect();
        int c=0;
        for(Boolean b : matches)
        {
            System.out.println(c++ + " " + b.toString());

        }
        long endTime = System.currentTimeMillis();
        System.out.println("Mapping to boolean similarity took " + Long.toString((endTime - startTime)/1000l) + " sec");

        baseclusterer bs = new baseclusterer();
        System.out.println("Calculating clusters.");
        startTime = System.currentTimeMillis();
        bs.calculateClusters(matches,RDDPairs);
        endTime = System.currentTimeMillis();
        System.out.println("Clustering took " + Long.toString((endTime - startTime)/1000l) + " sec");
        ArticlesPerCluster =  bs.getArticlesPerCluster();

    }
    //public void calculateClusters_graphs(JavaRDD<Tuple4<String, String, String, Long>> articles, List<Tuple2<Graph<String, Object>,Graph<String, Object>>> graphPairs) {
     public void calculateClusters_graphs(List<Tuple2<BDEArticle,BDEArticle>> articlePairs, List<Tuple2<Graph<String, Object>,Graph<String, Object>>> graphPairs) {

        // create pairs
        // graphs pairs are already generated
//        JavaPairRDD<Tuple4<String, String, String, Long>,Tuple4<String, String, String, Long>> articlePairs
//                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());

        // debug
        //StructUtils.printArticlePairs(RDDPairs, 5);
        // get matching mapping
        System.out.println("Mapping to boolean similarity...");
        long startTime = System.currentTimeMillis();

        // TODO: use flatMap?? we want for the full pairs rdd, each item mapped to a boolean value.
        List<Boolean> matches = new ArrayList<>();
         System.out.println("Calculate clusters with graphs, using " + mode + " " + simCutOff + " " + numPartitions);
        ExtractMatchingGraphPairsFunc comparator = new ExtractMatchingGraphPairsFunc(mode,simCutOff,numPartitions);

        try {
            for(int i=0;i<graphPairs.size();++i)
                    matches.add(comparator.call(graphPairs.get(i)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // spark parallelization ends here.
        // collect matches values



        int c=0;
        for(Boolean b : matches)
        {
            System.out.println(c++ + " " + b.toString());

        }
        long endTime = System.currentTimeMillis();
        System.out.println("Ttook " + Long.toString((endTime - startTime)/1000l) + " sec");

        baseclusterer bs = new baseclusterer();
        System.out.println("Calculating clusters.");
        startTime = System.currentTimeMillis();
        bs.calculateClusters_graphs(matches,articlePairs);
        endTime = System.currentTimeMillis();
        System.out.println("Took " + Long.toString((endTime - startTime)/1000l) + " sec");
        ArticlesPerCluster =  bs.getArticlesPerCluster();

    }

    public void calculateClusters_graphs_(JavaRDD<Tuple4<String, String, String, Long>> articles, JavaRDD<Tuple2<Graph<String, Object>,Graph<String, Object>>> graphPairs) {

        // create pairs
        // graphs pairs are already generated
        JavaPairRDD<Tuple4<String, String, String, Long>,Tuple4<String, String, String, Long>> articlePairs
                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());

        // debug
        //StructUtils.printArticlePairs(RDDPairs, 5);
        // get matching mapping
        System.out.println("Mapping to boolean similarity...");
        long startTime = System.currentTimeMillis();

        // TODO: use flatMap?? we want for the full pairs rdd, each item mapped to a boolean value.
        System.out.println("Calculate clusters with graphs, using " + mode + " " + simCutOff + " " + numPartitions);
        ExtractMatchingGraphPairsFunc comparator = new ExtractMatchingGraphPairsFunc(mode,simCutOff,numPartitions);

        JavaRDD<Boolean> machesrdd = graphPairs.map(new ExtractMatchingGraphPairsFunc(mode,simCutOff,numPartitions));
        List<Boolean> matches = machesrdd.collect();


        // spark parallelization ends here.
        // collect matches values



        int c=0;
        for(Boolean b : matches)
        {
            System.out.println(c++ + " " + b.toString());

        }
        long endTime = System.currentTimeMillis();
        System.out.println("Ttook " + Long.toString((endTime - startTime)/1000l) + " sec");

        baseclusterer bs = new baseclusterer();
        System.out.println("Calculating clusters.");
        startTime = System.currentTimeMillis();
        bs.calculateClusters_graphs_(matches,articlePairs);
        endTime = System.currentTimeMillis();
        System.out.println("Took " + Long.toString((endTime - startTime)/1000l) + " sec");
        ArticlesPerCluster =  bs.getArticlesPerCluster();

    }
    public void calculateClusters_graphs_2(JavaRDD<Tuple4<String, String, String, Long>> articles, List<Boolean> matches) {

        // create pairs
        // graphs pairs are already generated
        JavaPairRDD<Tuple4<String, String, String, Long>,Tuple4<String, String, String, Long>> articlePairs
                = articles.cartesian(articles).filter(new DocumentPairGenerationFilterFunction());


        baseclusterer bs = new baseclusterer();
        long startTime = System.currentTimeMillis();

        System.out.println("Calculating clusters.");
        bs.calculateClusters_graphs_(matches,articlePairs);
        long endTime = System.currentTimeMillis();

        System.out.println("Clustering took " + Long.toString((endTime - startTime)/1000l) + " sec");
        ArticlesPerCluster =  bs.getArticlesPerCluster();

    }
    private class baseclusterer {
        protected HashMap<Article, String> hsClusterPerArticle;
        protected HashMap<String, Topic> hsArticlesPerCluster;
        protected final Logger LOGGER = Logger.getAnonymousLogger();

        public Map<String, Topic> getArticlesPerCluster() {
            return (Map) (this.hsArticlesPerCluster != null && !this.hsArticlesPerCluster.isEmpty() ? this.hsArticlesPerCluster : Collections.EMPTY_MAP);
        }

        public void calculateClusters(List<Boolean> hmResults, JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs

        ) {

            this.hsArticlesPerCluster = new HashMap();
            this.hsClusterPerArticle = new HashMap();
            int count = -1;
            for (Tuple2<Tuple4<String,String,String,Long>,Tuple4<String,String,String,Long>> pair : RDDPairs.collect()) {
                //  The quadruple represents <entry_url, title, clean_text, timestamp>
                //    public Article(String sSource, String Title, String Text, String Category, String Feed, URLImage imageUrl, Date date) {

                Article aA = new Article(pair._1()._1(), pair._1()._2(), pair._1()._3(), "", "", new URLImage(""), new Date(pair._1()._4()));
                Article aB = new Article(pair._2()._1(), pair._2()._2(), pair._2()._3(), "", "", new URLImage(""), new Date(pair._2()._4()));

                boolean bMatch = hmResults.get(++count);
                String sClusterID;
                Topic tNew;
                if (bMatch) {
                    if (this.hsClusterPerArticle.containsKey(aA) && this.hsClusterPerArticle.containsKey(aB)) {
                        this.collapseTopics((String) this.hsClusterPerArticle.get(aA), (String) this.hsClusterPerArticle.get(aB));
                    } else {
                        if (!this.hsClusterPerArticle.containsKey(aA)) {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            tNew.add(aA);
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            this.hsClusterPerArticle.put(aA, sClusterID);
                        }

                        if (this.hsClusterPerArticle.containsKey(aB)) {
                            this.collapseTopics((String) this.hsClusterPerArticle.get(aA), (String) this.hsClusterPerArticle.get(aB));
                        } else {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            ((Topic) this.hsArticlesPerCluster.get(sClusterID)).add(aB);
                            this.hsClusterPerArticle.put(aB, sClusterID);
                        }
                    }
                } else {
                    if (!this.hsClusterPerArticle.containsKey(aA)) {
                        tNew = new Topic();
                        sClusterID = tNew.getID();
                        tNew.add(aA);
                        this.hsArticlesPerCluster.put(sClusterID, tNew);
                        this.hsClusterPerArticle.put(aA, sClusterID);
                    }

                    if (!this.hsClusterPerArticle.containsKey(aB)) {
                        tNew = new Topic();
                        sClusterID = tNew.getID();
                        tNew.add(aB);
                        this.hsArticlesPerCluster.put(sClusterID, tNew);
                        this.hsClusterPerArticle.put(aB, sClusterID);
                    }
                }
            }

            this.checkForInconsistencies();
            this.generateFinalTopics();
            return;
        }

        //public void calculateClusters(List<Boolean> hmResults, JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> RDDPairs
        public void calculateClusters_graphs(List<Boolean> hmResults, List<Tuple2<BDEArticle,BDEArticle>> ArticlePairs

        ) {

            this.hsArticlesPerCluster = new HashMap();
            this.hsClusterPerArticle = new HashMap();
            int count = -1;
            for (Tuple2<BDEArticle,BDEArticle> pair : ArticlePairs) {
                //  The quadruple represents <entry_url, title, clean_text, timestamp>
                //    public Article(String sSource, String Title, String Text, String Category, String Feed, URLImage imageUrl, Date date) {

//                Article aA = new Article(pair._1()._1(), pair._1()._2(), pair._1()._3(), "", "", new URLImage(""), new Date(pair._1()._4()));
//                Article aB = new Article(pair._2()._1(), pair._2()._2(), pair._2()._3(), "", "", new URLImage(""), new Date(pair._2()._4()));
                Article aA = pair._1();
                Article aB = pair._2();
                boolean bMatch = hmResults.get(++count);
                String sClusterID;
                Topic tNew;
                if (bMatch) {
                    if (this.hsClusterPerArticle.containsKey(aA) && this.hsClusterPerArticle.containsKey(aB)) {
                        this.collapseTopics((String) this.hsClusterPerArticle.get(aA), (String) this.hsClusterPerArticle.get(aB));
                    } else {
                        if (!this.hsClusterPerArticle.containsKey(aA)) {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            tNew.add(aA);
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            this.hsClusterPerArticle.put(aA, sClusterID);
                        }

                        if (this.hsClusterPerArticle.containsKey(aB)) {
                            this.collapseTopics((String) this.hsClusterPerArticle.get(aA), (String) this.hsClusterPerArticle.get(aB));
                        } else {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            ((Topic) this.hsArticlesPerCluster.get(sClusterID)).add(aB);
                            this.hsClusterPerArticle.put(aB, sClusterID);
                        }
                    }
                } else {
                    if (!this.hsClusterPerArticle.containsKey(aA)) {
                        tNew = new Topic();
                        sClusterID = tNew.getID();
                        tNew.add(aA);
                        this.hsArticlesPerCluster.put(sClusterID, tNew);
                        this.hsClusterPerArticle.put(aA, sClusterID);
                    }

                    if (!this.hsClusterPerArticle.containsKey(aB)) {
                        tNew = new Topic();
                        sClusterID = tNew.getID();
                        tNew.add(aB);
                        this.hsArticlesPerCluster.put(sClusterID, tNew);
                        this.hsClusterPerArticle.put(aB, sClusterID);
                    }
                }
            }

            this.checkForInconsistencies();
            this.generateFinalTopics();
            return;
        }

        public void calculateClusters_graphs_(List<Boolean> hmResults,  JavaPairRDD<Tuple4<String, String, String, Long>,Tuple4<String, String, String, Long>> ArticlePairs

        ) {

            this.hsArticlesPerCluster = new HashMap();
            this.hsClusterPerArticle = new HashMap();
            int count = -1;
            for (Tuple2<Tuple4<String,String,String,Long>,Tuple4<String,String,String,Long>> pair : ArticlePairs.collect()) {
                //  The quadruple represents <entry_url, title, clean_text, timestamp>
                //    public Article(String sSource, String Title, String Text, String Category, String Feed, URLImage imageUrl, Date date) {

                Article aA = new Article(pair._1()._1(), pair._1()._2(), pair._1()._3(), "", "", new URLImage(""), new Date(pair._1()._4()));
                Article aB = new Article(pair._2()._1(), pair._2()._2(), pair._2()._3(), "", "", new URLImage(""), new Date(pair._2()._4()));

                boolean bMatch = hmResults.get(++count);
                String sClusterID;
                Topic tNew;
                if (bMatch) {
                    if (this.hsClusterPerArticle.containsKey(aA) && this.hsClusterPerArticle.containsKey(aB)) {
                        this.collapseTopics((String) this.hsClusterPerArticle.get(aA), (String) this.hsClusterPerArticle.get(aB));
                    } else {
                        if (!this.hsClusterPerArticle.containsKey(aA)) {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            tNew.add(aA);
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            this.hsClusterPerArticle.put(aA, sClusterID);
                        }

                        if (this.hsClusterPerArticle.containsKey(aB)) {
                            this.collapseTopics((String) this.hsClusterPerArticle.get(aA), (String) this.hsClusterPerArticle.get(aB));
                        } else {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            ((Topic) this.hsArticlesPerCluster.get(sClusterID)).add(aB);
                            this.hsClusterPerArticle.put(aB, sClusterID);
                        }
                    }
                } else {
                    if (!this.hsClusterPerArticle.containsKey(aA)) {
                        tNew = new Topic();
                        sClusterID = tNew.getID();
                        tNew.add(aA);
                        this.hsArticlesPerCluster.put(sClusterID, tNew);
                        this.hsClusterPerArticle.put(aA, sClusterID);
                    }

                    if (!this.hsClusterPerArticle.containsKey(aB)) {
                        tNew = new Topic();
                        sClusterID = tNew.getID();
                        tNew.add(aB);
                        this.hsArticlesPerCluster.put(sClusterID, tNew);
                        this.hsClusterPerArticle.put(aB, sClusterID);
                    }
                }
            }

            this.checkForInconsistencies();
            this.generateFinalTopics();
            return;
        }

        protected void checkForInconsistencies() {
            int iCnt = 0;

            for (Article sCurCluster : hsClusterPerArticle.keySet()) {
                iCnt++;
                if (!((Topic) this.hsArticlesPerCluster.get(this.hsClusterPerArticle.get(sCurCluster))).contains(sCurCluster)) {
                    LOGGER.log(Level.SEVERE, "Mismatch found!");
                }
            }

            LOGGER.log(Level.INFO, "Checked {0} items.", Integer.valueOf(iCnt));


            for (String var6 : hsArticlesPerCluster.keySet()) {
                Topic T = hsArticlesPerCluster.get(var6);
                for (Article aCurArticle : T) {
                    if (((String) this.hsClusterPerArticle.get(aCurArticle)).trim().compareTo(var6.trim()) != 0) {
                        LOGGER.log(Level.SEVERE, "Mismatch found (reverse)!\n{0} != \n{1}\n", new Object[]{this.hsClusterPerArticle.get(aCurArticle), var6});
                    }

                }

                LOGGER.log(Level.INFO, "Reversed Checked Mappings Done");
            }
        }

        protected void generateFinalTopics() {

            HashSet hsIDs = new HashSet();
            HashMap hsFinalMap = new HashMap();
            HashMap hsClusterPerArticleFinal = new HashMap();
            Iterator mIter = this.hsArticlesPerCluster.entrySet().iterator();

            while (mIter.hasNext()) {
                Entry tmpEntry = (Entry) mIter.next();
                Topic tmpTopic = (Topic) tmpEntry.getValue();
                tmpTopic.setNewestDate(true);
                tmpTopic.setTitleFromNewestDate();
                tmpTopic.assignFinalTopicID();
                boolean bInsertedAsNew = hsIDs.add(tmpTopic.getID());
                if (!bInsertedAsNew) {
                    throw new RuntimeException("Found same ID for differrent topic..." + tmpTopic.toTopicDataJSON() + " : " + hsFinalMap.get(tmpTopic.getID()));
                }

                hsFinalMap.put(tmpTopic.getID(), tmpTopic);
                Iterator i$ = tmpTopic.iterator();

                while (i$.hasNext()) {
                    Article article = (Article) i$.next();
                    hsClusterPerArticleFinal.put(article, tmpTopic.getID());
                }

                mIter.remove();
            }

            this.hsArticlesPerCluster = (HashMap) hsFinalMap;
            this.hsClusterPerArticle = (HashMap) hsClusterPerArticleFinal;
        }
        protected boolean collapseTopics(String sTopic1ID, String sTopic2ID) {
            Topic t1 = (Topic) this.hsArticlesPerCluster.get(sTopic1ID);
            Topic t2 = (Topic) this.hsArticlesPerCluster.get(sTopic2ID);
            if (t1 == t2) {
                return false;
            } else {
                Iterator i$ = t2.iterator();

                while (i$.hasNext()) {
                    Article aCur = (Article) i$.next();
                    t1.add(aCur);
                    this.hsClusterPerArticle.put(aCur, t1.getID());
                    this.hsArticlesPerCluster.put(t1.getID(), t1);
                }

                t2.clear();
                this.hsArticlesPerCluster.remove(t2.getID());
                return true;
            }
        }
    }
}