package gr.demokritos.iit.clustering.clustering;

import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import gr.demokritos.iit.jinsect.structs.Pair;
import org.scify.newsum.server.clustering.BaseArticleClusterer;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.model.structures.URLImage;
import scala.Tuple2;
import scala.Tuple4;

/**
 * Created by nik on 11/17/16.
 */
public class ParameterizedBaseArticleClusterer extends BaseArticleClusterer {

    // storage variables for "phase" mode
//    protected HashMap<String, Topic> hsArticlesPerCluster;
//    protected HashMap<Article, String> hsClusterPerArticle;
    double NVSThreshold;

    public List<Article> getProcessedCache() {
        return ProcessedCache;
    }

    List<Article> ProcessedCache;
    public ParameterizedBaseArticleClusterer(List<? extends Article> lsArticles, double thresh) {
        super(lsArticles);
        NVSThreshold = thresh;
    }

    public ParameterizedBaseArticleClusterer(double thresh) {
        super(new ArrayList());
        NVSThreshold = thresh;
    }

    @Override
    public boolean getMatch(Article aA, Article aB) {
        if("SciFY News".equals(aA.getCategory())) {
            return false;
        } else {
            GraphSimilarity gs = this.compareArticles(aA, aB);
            double NVS = gs.SizeSimilarity == 0.0D?0.0D:gs.ValueSimilarity / gs.SizeSimilarity;
            boolean bMatch = NVS > NVSThreshold && gs.SizeSimilarity > 0.1D;
            boolean TitleMatch = this.isPossiblySameSentence(aA.getTitle(), aB.getTitle());
            //System.out.println(String.format("%s - %s ||| nvs: %f , gs: %f , returning %d",aA.getTitle(),aB.getTitle(),NVS,gs.SizeSimilarity,bMatch || TitleMatch));
            return bMatch || TitleMatch;
        }
    }

    public HashMap<Tuple2<Article,Article>,Boolean> mapToBoolean(List<Article> articles)
    {
        List lsPairs = this.getPairs(articles);
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final ConcurrentHashMap hmResults = new ConcurrentHashMap();
        LOGGER.log(Level.INFO, "Examining pairs...");
        Iterator i$ = lsPairs.iterator();


        while(i$.hasNext()) {
            final Pair p = (Pair)i$.next();
            es.submit(new Runnable() {
                public void run() {
                    Article aA = (Article)p.getFirst();
                    Article aB = (Article)p.getSecond();
                    boolean bMatch = getMatch(aA, aB);
                    ConcurrentHashMap var4 = hmResults;
                    synchronized(hmResults) {
                        hmResults.put(p, Boolean.valueOf(bMatch));
                    }
                }
            });
        }

        es.shutdown();

        try {
            es.awaitTermination(1L, TimeUnit.DAYS);
            LOGGER.log(Level.INFO, "Examining pairs DONE.");
        } catch (InterruptedException var11) {
            LOGGER.log(Level.SEVERE, var11.getMessage(), var11);
            return null;
        }

        HashMap<Tuple2<Article,Article>,Boolean> res = new HashMap<>();
        for(Object pp : hmResults.keySet())
        {

            Pair artpair = (Pair<Article,Article>) pp;
            Tuple2<Article,Article> arttuple = new Tuple2<>((Article)artpair.getFirst(),(Article)artpair.getSecond());
            res.put(arttuple,(Boolean)hmResults.get(pp));
        }

        return res;
    }

    public void onlyCluster(HashMap<Tuple2<
            Tuple4<String, String, String, Long>,
            Tuple4<String, String, String, Long>>,Boolean> articlePairsToBoolean) {

        ProcessedCache = new ArrayList<>();
        //  The quadruple represents <entry_url, title, clean_text, timestamp>


        if(hsClusterPerArticle == null) hsClusterPerArticle = new HashMap<>();
        if(hsArticlesPerCluster == null) hsArticlesPerCluster = new HashMap<>();
        hsClusterPerArticle.clear();
        hsArticlesPerCluster.clear();
        Iterator i$ = articlePairsToBoolean.keySet().iterator();
        while(true) {
            while(true) {
                while(i$.hasNext()) {
                    Tuple2< Tuple4<String, String, String, Long>,
                            Tuple4<String, String, String, Long>> t  =
                            (Tuple2<
                                    Tuple4<String, String, String, Long>,
                                    Tuple4<String, String, String, Long>
                                    >) i$.next();


                    Article aA = new Article(t._1()._1(), t._1()._2(), t._1()._3(), "", "", new URLImage(""), new Date(t._1()._4()));
                    Article aB = new Article(t._2()._1(), t._2()._2(), t._2()._3(), "", "", new URLImage(""), new Date(t._2()._4()));

                    if(!ProcessedCache.contains(aA)) ProcessedCache.add( aA);
                    if(!ProcessedCache.contains(aB)) ProcessedCache.add( aB);
                    //boolean bMatch = ((Boolean)articlePairsToBoolean.get(p)).booleanValue();
                    boolean bMatch = ((Boolean)articlePairsToBoolean.get(t)).booleanValue();
                    String sClusterID;
                    Topic tNew;
                    if(bMatch) {
                        if(this.hsClusterPerArticle.containsKey(aA) && this.hsClusterPerArticle.containsKey(aB)) {
                            this.collapseTopics((String)this.hsClusterPerArticle.get(aA), (String)this.hsClusterPerArticle.get(aB));
                        } else {
                            if(!this.hsClusterPerArticle.containsKey(aA)) {
                                tNew = new Topic();
                                sClusterID = tNew.getID();
                                tNew.add(aA);
                                this.hsArticlesPerCluster.put(sClusterID, tNew);
                                this.hsClusterPerArticle.put(aA, sClusterID);
                            }

                            if(this.hsClusterPerArticle.containsKey(aB)) {
                                this.collapseTopics((String)this.hsClusterPerArticle.get(aA), (String)this.hsClusterPerArticle.get(aB));
                            } else {
                                tNew = new Topic();
                                sClusterID = tNew.getID();
                                this.hsArticlesPerCluster.put(sClusterID, tNew);
                                ((Topic)this.hsArticlesPerCluster.get(sClusterID)).add(aB);
                                this.hsClusterPerArticle.put(aB, sClusterID);
                            }
                        }
                    } else {
                        if(!this.hsClusterPerArticle.containsKey(aA)) {
                            tNew = new Topic();
                            sClusterID = tNew.getID();
                            tNew.add(aA);
                            this.hsArticlesPerCluster.put(sClusterID, tNew);
                            this.hsClusterPerArticle.put(aA, sClusterID);
                        }

                        if(!this.hsClusterPerArticle.containsKey(aB)) {
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
        }
    }

}
