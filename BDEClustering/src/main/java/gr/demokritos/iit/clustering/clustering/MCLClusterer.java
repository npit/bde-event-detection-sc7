package gr.demokritos.iit.clustering.clustering;

import Jama.Matrix;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import org.apache.spark.api.java.JavaRDD;
import org.scify.newsum.server.model.datacollections.Articles;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Topic;
import scala.Tuple4;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by nik on 11/17/16.
 */
public class MCLClusterer implements IClusterer
{

    protected HashMap<String, Topic> hsArticlesPerCluster;
    protected HashMap<String, Topic> PreviousClusteredTopics;
    protected HashMap<Article, String> hsClusterPerArticle;

    Articles articles;
    double CutOff;
    public MCLClusterer(Articles argArticles, double argCutoff)
    {
        hsClusterPerArticle = new HashMap<>();
        hsArticlesPerCluster = new HashMap<>();

        articles = argArticles;
        CutOff = argCutoff;
    }

    protected Matrix normalizeMatrixPerColumn(Matrix mToNormalize, double dPower) {
        for(int iColumnCnt = 0; iColumnCnt < mToNormalize.getColumnDimension(); ++iColumnCnt) {
            double dColSum = 0.0D;

            int iRowCnt;
            double dNormalized;
            for(iRowCnt = 0; iRowCnt < mToNormalize.getRowDimension(); ++iRowCnt) {
                dNormalized = Math.pow(mToNormalize.get(iRowCnt, iColumnCnt), dPower);
                mToNormalize.set(iRowCnt, iColumnCnt, dNormalized);
                dColSum += dNormalized;
            }

            for(iRowCnt = 0; iRowCnt < mToNormalize.getRowDimension(); ++iRowCnt) {
                dNormalized = mToNormalize.get(iRowCnt, iColumnCnt) / dColSum;
                mToNormalize.set(iRowCnt, iColumnCnt, dNormalized);
            }
        }

        return mToNormalize;
    }



    protected Matrix getSimilarityMatrix(List<Article> lAllArticles) {
        final Matrix mSims = new Matrix(lAllArticles.size(), lAllArticles.size());
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final List lAllArticlesArg = lAllArticles;
        final int iFirstCnt = 0;
        int idx = 0;
        for(Iterator ex = lAllArticles.iterator(); ex.hasNext(); ++idx) {
            final Article aFirst = (Article)ex.next();
            es.submit(new Runnable() {
                public void run() {
                    int iSecondCnt = 0;
                    new NGramCachedGraphComparator();
                    DocumentNGramSymWinGraph gFirst = new DocumentNGramSymWinGraph();
                    gFirst.setDataString(aFirst.getDescription());

                    for(Iterator i$ = lAllArticlesArg.iterator(); i$.hasNext(); ++iSecondCnt) {
                        Article aSecond = (Article)i$.next();
                        double dSim;
                        if(iSecondCnt == iFirstCnt) {
                            dSim = 1.0D;
                        } else {
                            label44: {
                                label35: {
                                    if(aFirst.getCategory() == null) {
                                        if(aSecond.getCategory() != null) {
                                            break label35;
                                        }
                                    } else if(!aFirst.getCategory().equals(aSecond.getCategory())) {
                                        break label35;
                                    }

                                    dSim = MCLClusterer.this.articleSimilarity(aFirst, aSecond);
                                    break label44;
                                }

                                dSim = 0.0D;
                            }
                        }

                        Matrix var8 = mSims;
                        synchronized(mSims) {
                            mSims.set(iFirstCnt, iSecondCnt, dSim);
                        }
                    }

                }
            });
        }

        es.shutdown();

        try {
            es.awaitTermination(1L, TimeUnit.DAYS);
            return mSims;
        } catch (InterruptedException var9) {
            return null;
        }
    }

    public double articleSimilarity(Article a, Article b) {
        GraphSimilarity gs = compareArticles(a, b);
        return gs.SizeSimilarity == 0.0D?0.0D:gs.ValueSimilarity / gs.SizeSimilarity;
    }

    public GraphSimilarity compareArticles(Article aOne, Article aTwo) {
        DocumentWordGraph dgFirstGraph = new DocumentWordGraph();
        DocumentWordGraph dgSecondGraph = new DocumentWordGraph();
        dgFirstGraph.WordEvaluator = new WordEvaluatorListener() {
            public boolean evaluateWord(String string) {
                return string.length() > 3 && string.matches("\\p{javaUpperCase}+.*");
            }
        };
        dgSecondGraph.WordEvaluator = dgFirstGraph.WordEvaluator;
        dgFirstGraph.setDataString(aOne.getTitle() + " " + aOne.getDescription());
        dgSecondGraph.setDataString(aTwo.getTitle() + " " + aTwo.getDescription());
        NGramCachedGraphComparator ngc = new NGramCachedGraphComparator();
        return ngc.getSimilarityBetween(dgFirstGraph, dgSecondGraph);
    }

    protected HashMap<String, Topic> getArticleTopics(Articles origArticles) {
        HashMap hsRes = new HashMap();
        Matrix mSims = this.getSimilarityMatrix(origArticles);
        this.normalizeMatrixPerColumn(mSims, 1.0D);
        Matrix mLastRes = null;

        int iRow;
        for(iRow = 0; iRow < 100; ++iRow) {
            mLastRes = mSims.times(mSims);
            this.normalizeMatrixPerColumn(mLastRes, 2.0D);
            if(mSims.minus(mLastRes).normInf() < 0.001D) {
                break;
            }

            mSims = mLastRes.copy();
        }

        for(iRow = 0; iRow < mLastRes.getRowDimension(); ++iRow) {
            Topic tCur = new Topic();

            for(int iCol = 0; iCol < mLastRes.getColumnDimension(); ++iCol) {
                if(mLastRes.get(iRow, iCol) > 0.01D) {
                    tCur.add((Article)origArticles.get(iCol));
                }
            }

            if(!tCur.isEmpty()) {
                hsRes.put(tCur.getID(), tCur);
            }
        }

        return hsRes;
    }

    @Override
    public void calculateClusters(JavaRDD<Tuple4<String, String, String, Long>> articles) {

    }

    @Override
    public void calculateClusters(Articles articles) {
        this.hsArticlesPerCluster = this.getArticleTopics(articles);
        Iterator ex = this.hsArticlesPerCluster.entrySet().iterator();

        Map.Entry mp;
        while(ex.hasNext()) {
            mp = (Map.Entry)ex.next();
            Iterator tmpTopic = ((Topic)mp.getValue()).iterator();

            while(tmpTopic.hasNext()) {
                Article aCur = (Article)tmpTopic.next();
                this.hsClusterPerArticle.put(aCur, (String) mp.getKey());
            }
        }

        ex = this.hsArticlesPerCluster.entrySet().iterator();

        while(ex.hasNext()) {
            mp = (Map.Entry)ex.next();
            Topic tmpTopic1 = (Topic)mp.getValue();
            if(!tmpTopic1.isEmpty()) {
                tmpTopic1.setNewestDate(true);
                tmpTopic1.setTitleFromNewestDate();
            }
        }

    }

    public Map<String, Topic> getArticlesPerCluster()
    {
        return this.hsArticlesPerCluster;
    }
}
